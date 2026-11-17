package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.support.BackendApiIntegrationTestSupport;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserRole;
import br.com.curralzap.backend.whatsapp.application.DownloadedWhatsappAudio;
import br.com.curralzap.backend.whatsapp.application.StoreWhatsappAudioRequest;
import br.com.curralzap.backend.whatsapp.application.TranscodedWhatsappAudio;
import br.com.curralzap.backend.whatsapp.application.WhatsappAudioDownloadGateway;
import br.com.curralzap.backend.whatsapp.application.WhatsappAudioDownloadService;
import br.com.curralzap.backend.whatsapp.application.WhatsappAudioPayload;
import br.com.curralzap.backend.whatsapp.application.WhatsappAudioTranscodeRequest;
import br.com.curralzap.backend.whatsapp.application.WhatsappAudioTranscodeRequestedEvent;
import br.com.curralzap.backend.whatsapp.application.WhatsappAudioTranscodeRequestedPublisher;
import br.com.curralzap.backend.whatsapp.application.WhatsappAudioTranscodeService;
import br.com.curralzap.backend.whatsapp.application.WhatsappAudioTranscoder;
import br.com.curralzap.backend.whatsapp.application.WhatsappAudioTranscriptionRequest;
import br.com.curralzap.backend.whatsapp.application.WhatsappAudioTranscriptionRequestedEvent;
import br.com.curralzap.backend.whatsapp.application.WhatsappAudioTranscriptionRequestedPublisher;
import br.com.curralzap.backend.whatsapp.application.WhatsappAudioTranscriptionResult;
import br.com.curralzap.backend.whatsapp.application.WhatsappAudioTranscriptionService;
import br.com.curralzap.backend.whatsapp.application.WhatsappAudioTranscriber;
import br.com.curralzap.backend.whatsapp.application.WhatsappAudioStorage;
import br.com.curralzap.backend.whatsapp.application.WhatsappButtonConfirmationRequestedEvent;
import br.com.curralzap.backend.whatsapp.application.WhatsappButtonConfirmationRequestedPublisher;
import br.com.curralzap.backend.whatsapp.application.WhatsappMessageReceivedEvent;
import br.com.curralzap.backend.whatsapp.application.WhatsappOutboundDeliveryRequestedEvent;
import br.com.curralzap.backend.whatsapp.application.WhatsappOutboundDeliveryRequestedPublisher;
import br.com.curralzap.backend.whatsapp.application.WhatsappOutboundDeliveryResult;
import br.com.curralzap.backend.whatsapp.application.WhatsappOutboundDeliveryService;
import br.com.curralzap.backend.whatsapp.application.WhatsappOutboundGateway;
import br.com.curralzap.backend.whatsapp.application.WhatsappPayloadValidationRequestedEvent;
import br.com.curralzap.backend.whatsapp.application.WhatsappPayloadValidationRequestedPublisher;
import br.com.curralzap.backend.whatsapp.application.WhatsappPayloadValidationService;
import br.com.curralzap.backend.whatsapp.application.WhatsappReceivedMessagePublisher;
import br.com.curralzap.backend.whatsapp.application.WhatsappButtonsOutboundMessage;
import br.com.curralzap.backend.whatsapp.application.WhatsappTextOutboundMessage;
import br.com.curralzap.backend.whatsapp.application.WhatsappTranscriptExtractionRequestedEvent;
import br.com.curralzap.backend.whatsapp.application.WhatsappTranscriptExtractionRequestedPublisher;
import br.com.curralzap.backend.whatsapp.application.WhatsappTranscriptExtractionResult;
import br.com.curralzap.backend.whatsapp.application.WhatsappTranscriptExtractionService;
import br.com.curralzap.backend.whatsapp.application.WhatsappTranscriptExtractor;
import br.com.curralzap.backend.whatsapp.application.WhatsappWorkerRetryableException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("worker")
@Import(WhatsappPipelineRegressionIntegrationTest.TestPipelineConfiguration.class)
class WhatsappPipelineRegressionIntegrationTest extends BackendApiIntegrationTestSupport {

    private static final String SECRET = "curralzap-test-whatsapp-webhook-secret";
    private static final String TRANSCRIPTION_TOPIC = "whatsapp.audio.transcription.requested.test";

    @Autowired
    private WhatsappAudioDownloadService audioDownloadService;

    @Autowired
    private WhatsappAudioTranscodeService audioTranscodeService;

    @Autowired
    private WhatsappAudioTranscriptionService audioTranscriptionService;

    @Autowired
    private WhatsappTranscriptExtractionService transcriptExtractionService;

    @Autowired
    private WhatsappPayloadValidationService payloadValidationService;

    @Autowired
    private WhatsappOutboundDeliveryService outboundDeliveryService;

    @Autowired
    private WhatsappKafkaDeadLetterRecoverer deadLetterRecoverer;

    @Autowired
    private CapturingWhatsappReceivedMessagePublisher receivedPublisher;

    @Autowired
    private CapturingWhatsappAudioTranscodeRequestedPublisher audioTranscodePublisher;

    @Autowired
    private CapturingWhatsappAudioTranscriptionRequestedPublisher audioTranscriptionPublisher;

    @Autowired
    private CapturingWhatsappTranscriptExtractionRequestedPublisher transcriptExtractionPublisher;

    @Autowired
    private CapturingWhatsappPayloadValidationRequestedPublisher payloadValidationPublisher;

    @Autowired
    private CapturingWhatsappOutboundDeliveryRequestedPublisher outboundDeliveryPublisher;

    @Autowired
    private CapturingWhatsappButtonConfirmationRequestedPublisher buttonConfirmationPublisher;

    @Autowired
    private FakeWhatsappAudioDownloadGateway downloadGateway;

    @Autowired
    private FakeWhatsappAudioStorage audioStorage;

    @Autowired
    private FakeWhatsappAudioTranscoder audioTranscoder;

    @Autowired
    private FakeWhatsappAudioTranscriber audioTranscriber;

    @Autowired
    private FakeWhatsappTranscriptExtractor transcriptExtractor;

    @Autowired
    private FakeWhatsappOutboundGateway outboundGateway;

    @Autowired
    private KafkaTemplate<Object, Object> deadLetterKafkaTemplate;

    @BeforeEach
    void resetFixture() {
        receivedPublisher.events.clear();
        audioTranscodePublisher.events.clear();
        audioTranscriptionPublisher.events.clear();
        transcriptExtractionPublisher.events.clear();
        payloadValidationPublisher.events.clear();
        outboundDeliveryPublisher.events.clear();
        buttonConfirmationPublisher.events.clear();
        downloadGateway.responses.clear();
        audioStorage.files.clear();
        audioTranscoder.failure = null;
        audioTranscriber.failure = null;
        audioTranscriber.results.clear();
        transcriptExtractor.results.clear();
        outboundGateway.failure = null;
        outboundGateway.sentMessages.clear();
        outboundGateway.sentButtonMessages.clear();
        reset(deadLetterKafkaTemplate);
    }

    @Test
    void audioWebhookFlowsThroughWorkersAndSupportDetailReflectsAwaitingConfirmation() throws Exception {
        UUID farmId = insertFarm("Fazenda Pipeline");
        UserAccount admin = insertUser(farmId, "5511999998101", "Admin Pipeline", UserRole.ADMIN, true);
        UUID animalId = insertAnimal(farmId, null, "347");
        String providerMessageId = "wamid.pipeline.ok";
        String mediaUrl = "https://media.example/audio-ok.ogg";

        configureHappyPath(providerMessageId, mediaUrl, animalId);

        postAudioWebhook("req-qa-007-ok", providerMessageId, admin.phone(), mediaUrl)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("accepted"));

        runHappyPathPipeline();

        assertInboundStatus(providerMessageId, "AWAITING_CONFIRMATION");
        assertThat(outboundGateway.sentButtonMessages).singleElement()
                .extracting(WhatsappButtonsOutboundMessage::quotedProviderMessageId)
                .isEqualTo(providerMessageId);
        assertThat(countOutboundMessages(providerMessageId)).isEqualTo(1);

        String accessToken = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));
        UUID inboundMessageId = inboundMessageId(providerMessageId);
        mockMvc.perform(get("/api/v1/whatsapp/messages/{messageId}", inboundMessageId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("awaiting_confirmation"))
                .andExpect(jsonPath("$.retry.supported").value(false))
                .andExpect(jsonPath("$.transcript.transcript").value("Boi 347 pesou 520 quilos"))
                .andExpect(jsonPath("$.extractedPayload.intent").value("register_weighing"))
                .andExpect(jsonPath("$.outboundMessages[0].status").value("sent"));
    }

    @Test
    void deadLetteredTranscriptionFailureCanBeRetriedThroughSupportEndpoint() throws Exception {
        UUID farmId = insertFarm("Fazenda Retry Pipeline");
        UserAccount admin = insertUser(farmId, "5511999998102", "Admin Retry Pipeline", UserRole.ADMIN, true);
        UUID animalId = insertAnimal(farmId, null, "512");
        String providerMessageId = "wamid.pipeline.retry";
        String mediaUrl = "https://media.example/audio-retry.ogg";

        configureHappyPath(providerMessageId, mediaUrl, animalId);
        audioTranscriber.failure = new IllegalStateException("OpenAI unavailable.");

        postAudioWebhook("req-qa-007-retry", providerMessageId, admin.phone(), mediaUrl)
                .andExpect(status().isAccepted());

        audioDownloadService.handleReceivedMessage(single(receivedPublisher.events));
        audioTranscodeService.handleRequestedTranscode(single(audioTranscodePublisher.events));

        WhatsappAudioTranscriptionRequestedEvent transcriptionEvent = single(audioTranscriptionPublisher.events);
        audioTranscriptionPublisher.events.clear();

        assertThatThrownBy(() -> audioTranscriptionService.handleRequestedTranscription(transcriptionEvent))
                .isInstanceOf(WhatsappWorkerRetryableException.class);

        deadLetterRecoverer.accept(
                new ConsumerRecord<>(TRANSCRIPTION_TOPIC, 0, 0L, providerMessageId, transcriptionEvent),
                new IllegalStateException("Retry attempts exhausted.")
        );

        verify(deadLetterKafkaTemplate).send(TRANSCRIPTION_TOPIC + ".dlt", providerMessageId, transcriptionEvent);

        String accessToken = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));
        UUID inboundMessageId = inboundMessageId(providerMessageId);

        mockMvc.perform(get("/api/v1/whatsapp/messages/{messageId}", inboundMessageId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.retry.supported").value(true))
                .andExpect(jsonPath("$.retry.action").value("audio_transcription_requested"));

        audioTranscriber.failure = null;

        mockMvc.perform(post("/api/v1/whatsapp/messages/{messageId}/retry", inboundMessageId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.action").value("audio_transcription_requested"));

        WhatsappAudioTranscriptionRequestedEvent retriedEvent = single(audioTranscriptionPublisher.events);
        audioTranscriptionService.handleRequestedTranscription(retriedEvent);
        transcriptExtractionService.handleRequestedExtraction(single(transcriptExtractionPublisher.events));
        payloadValidationService.handleRequestedValidation(single(payloadValidationPublisher.events));
        outboundDeliveryService.handleRequestedDelivery(single(outboundDeliveryPublisher.events));

        assertInboundStatus(providerMessageId, "AWAITING_CONFIRMATION");
        assertThat(outboundGateway.sentButtonMessages).hasSize(1);
    }

    @Test
    void duplicateWebhookAfterSuccessfulPipelineDoesNotCreateExtraDownstreamWork() throws Exception {
        UUID farmId = insertFarm("Fazenda Duplicate Pipeline");
        UserAccount admin = insertUser(farmId, "5511999998103", "Admin Duplicate Pipeline", UserRole.ADMIN, true);
        UUID animalId = insertAnimal(farmId, null, "900");
        String providerMessageId = "wamid.pipeline.duplicate";
        String mediaUrl = "https://media.example/audio-duplicate.ogg";

        configureHappyPath(providerMessageId, mediaUrl, animalId);

        postAudioWebhook("req-qa-007-dup-1", providerMessageId, admin.phone(), mediaUrl)
                .andExpect(status().isAccepted());

        runHappyPathPipeline();

        postAudioWebhook("req-qa-007-dup-2", providerMessageId, admin.phone(), mediaUrl)
                .andExpect(status().isAccepted());

        assertThat(receivedPublisher.events).hasSize(1);
        assertThat(countInboundMessages(providerMessageId)).isEqualTo(1);
        assertThat(countOutboundMessages(providerMessageId)).isEqualTo(1);
        assertInboundStatus(providerMessageId, "AWAITING_CONFIRMATION");
    }

    private void configureHappyPath(String providerMessageId, String mediaUrl, UUID animalId) {
        downloadGateway.responses.put(
                mediaUrl,
                new DownloadedWhatsappAudio("audio/ogg", "downloaded-audio".getBytes(StandardCharsets.UTF_8))
        );
        audioTranscriber.results.put(
                providerMessageId,
                new WhatsappAudioTranscriptionResult("openai", "gpt-4o-mini-transcribe", "Boi 347 pesou 520 quilos")
        );
        transcriptExtractor.results.put(
                providerMessageId,
                WhatsappTranscriptExtractionResult.supported(
                        "openai",
                        "gpt-5.4-mini",
                        "register_weighing",
                        Map.of("animalId", animalId.toString(), "weightKg", 520),
                        0.98
                )
        );
    }

    private org.springframework.test.web.servlet.ResultActions postAudioWebhook(
            String requestId,
            String providerMessageId,
            String phoneNumber,
            String mediaUrl
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/whatsapp/webhooks/evolution")
                .header("X-Request-Id", requestId)
                .header("X-Webhook-Secret", SECRET)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "event": "messages.upsert",
                          "data": {
                            "key": {
                              "id": "%s",
                              "remoteJid": "%s@s.whatsapp.net",
                              "fromMe": false
                            },
                            "messageType": "audioMessage",
                            "message": {
                              "audioMessage": {
                                "url": "%s",
                                "mimetype": "audio/ogg"
                              }
                            }
                          }
                        }
                        """.formatted(providerMessageId, phoneNumber, mediaUrl)));
    }

    private void runHappyPathPipeline() {
        audioDownloadService.handleReceivedMessage(single(receivedPublisher.events));
        audioTranscodeService.handleRequestedTranscode(single(audioTranscodePublisher.events));
        audioTranscriptionService.handleRequestedTranscription(single(audioTranscriptionPublisher.events));
        transcriptExtractionService.handleRequestedExtraction(single(transcriptExtractionPublisher.events));
        payloadValidationService.handleRequestedValidation(single(payloadValidationPublisher.events));
        outboundDeliveryService.handleRequestedDelivery(single(outboundDeliveryPublisher.events));
    }

    private void assertInboundStatus(String providerMessageId, String status) {
        assertThat(jdbcTemplate.queryForObject(
                "select status from whatsapp_inbound_messages where provider_message_id = ?",
                String.class,
                providerMessageId
        )).isEqualTo(status);
    }

    private UUID inboundMessageId(String providerMessageId) {
        return UUID.fromString(jdbcTemplate.queryForObject(
                "select id from whatsapp_inbound_messages where provider_message_id = ?",
                String.class,
                providerMessageId
        ));
    }

    private int countInboundMessages(String providerMessageId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from whatsapp_inbound_messages where provider_message_id = ?",
                Integer.class,
                providerMessageId
        );
        return count == null ? 0 : count;
    }

    private int countOutboundMessages(String providerMessageId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from whatsapp_outbound_messages outbox
                        join whatsapp_inbound_messages inbound on inbound.id = outbox.inbound_message_id
                        where inbound.provider_message_id = ?
                        """,
                Integer.class,
                providerMessageId
        );
        return count == null ? 0 : count;
    }

    private static <T> T single(List<T> items) {
        assertThat(items).hasSize(1);
        return items.getFirst();
    }

    @TestConfiguration
    static class TestPipelineConfiguration {

        @Bean
        @Primary
        CapturingWhatsappReceivedMessagePublisher capturingWhatsappReceivedMessagePublisher() {
            return new CapturingWhatsappReceivedMessagePublisher();
        }

        @Bean
        @Primary
        CapturingWhatsappAudioTranscodeRequestedPublisher capturingWhatsappAudioTranscodeRequestedPublisher() {
            return new CapturingWhatsappAudioTranscodeRequestedPublisher();
        }

        @Bean
        @Primary
        CapturingWhatsappAudioTranscriptionRequestedPublisher capturingWhatsappAudioTranscriptionRequestedPublisher() {
            return new CapturingWhatsappAudioTranscriptionRequestedPublisher();
        }

        @Bean
        @Primary
        CapturingWhatsappTranscriptExtractionRequestedPublisher capturingWhatsappTranscriptExtractionRequestedPublisher() {
            return new CapturingWhatsappTranscriptExtractionRequestedPublisher();
        }

        @Bean
        @Primary
        CapturingWhatsappPayloadValidationRequestedPublisher capturingWhatsappPayloadValidationRequestedPublisher() {
            return new CapturingWhatsappPayloadValidationRequestedPublisher();
        }

        @Bean
        @Primary
        CapturingWhatsappOutboundDeliveryRequestedPublisher capturingWhatsappOutboundDeliveryRequestedPublisher() {
            return new CapturingWhatsappOutboundDeliveryRequestedPublisher();
        }

        @Bean
        @Primary
        CapturingWhatsappButtonConfirmationRequestedPublisher capturingWhatsappButtonConfirmationRequestedPublisher() {
            return new CapturingWhatsappButtonConfirmationRequestedPublisher();
        }

        @Bean
        @Primary
        FakeWhatsappAudioDownloadGateway fakeWhatsappAudioDownloadGateway() {
            return new FakeWhatsappAudioDownloadGateway();
        }

        @Bean
        @Primary
        FakeWhatsappAudioStorage fakeWhatsappAudioStorage() {
            return new FakeWhatsappAudioStorage();
        }

        @Bean
        @Primary
        FakeWhatsappAudioTranscoder fakeWhatsappAudioTranscoder() {
            return new FakeWhatsappAudioTranscoder();
        }

        @Bean
        @Primary
        FakeWhatsappAudioTranscriber fakeWhatsappAudioTranscriber() {
            return new FakeWhatsappAudioTranscriber();
        }

        @Bean
        @Primary
        FakeWhatsappTranscriptExtractor fakeWhatsappTranscriptExtractor() {
            return new FakeWhatsappTranscriptExtractor();
        }

        @Bean
        @Primary
        FakeWhatsappOutboundGateway fakeWhatsappOutboundGateway() {
            return new FakeWhatsappOutboundGateway();
        }

        @Bean
        @Primary
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, WhatsappMessageReceivedEvent> whatsappMessageReceivedKafkaTemplate() {
            return org.mockito.Mockito.mock(KafkaTemplate.class);
        }

        @Bean
        @Primary
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, WhatsappAudioTranscodeRequestedEvent> whatsappAudioTranscodeRequestedKafkaTemplate() {
            return org.mockito.Mockito.mock(KafkaTemplate.class);
        }

        @Bean
        @Primary
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, WhatsappAudioTranscriptionRequestedEvent> whatsappAudioTranscriptionRequestedKafkaTemplate() {
            return org.mockito.Mockito.mock(KafkaTemplate.class);
        }

        @Bean
        @Primary
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, WhatsappTranscriptExtractionRequestedEvent> whatsappTranscriptExtractionRequestedKafkaTemplate() {
            return org.mockito.Mockito.mock(KafkaTemplate.class);
        }

        @Bean
        @Primary
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, WhatsappPayloadValidationRequestedEvent> whatsappPayloadValidationRequestedKafkaTemplate() {
            return org.mockito.Mockito.mock(KafkaTemplate.class);
        }

        @Bean
        @Primary
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, WhatsappOutboundDeliveryRequestedEvent> whatsappOutboundDeliveryRequestedKafkaTemplate() {
            return org.mockito.Mockito.mock(KafkaTemplate.class);
        }

        @Bean
        @Primary
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, WhatsappButtonConfirmationRequestedEvent> whatsappButtonConfirmationRequestedKafkaTemplate() {
            return org.mockito.Mockito.mock(KafkaTemplate.class);
        }

        @Bean
        @Primary
        @SuppressWarnings("unchecked")
        KafkaTemplate<Object, Object> deadLetterKafkaTemplate() {
            return org.mockito.Mockito.mock(KafkaTemplate.class);
        }
    }

    static final class CapturingWhatsappReceivedMessagePublisher implements WhatsappReceivedMessagePublisher {

        private final List<WhatsappMessageReceivedEvent> events = new ArrayList<>();

        @Override
        public void publishReceived(WhatsappMessageReceivedEvent event) {
            events.add(event);
        }
    }

    static final class CapturingWhatsappAudioTranscodeRequestedPublisher implements WhatsappAudioTranscodeRequestedPublisher {

        private final List<WhatsappAudioTranscodeRequestedEvent> events = new ArrayList<>();

        @Override
        public void publishRequested(WhatsappAudioTranscodeRequestedEvent event) {
            events.add(event);
        }
    }

    static final class CapturingWhatsappAudioTranscriptionRequestedPublisher implements WhatsappAudioTranscriptionRequestedPublisher {

        private final List<WhatsappAudioTranscriptionRequestedEvent> events = new ArrayList<>();

        @Override
        public void publishRequested(WhatsappAudioTranscriptionRequestedEvent event) {
            events.add(event);
        }
    }

    static final class CapturingWhatsappTranscriptExtractionRequestedPublisher implements WhatsappTranscriptExtractionRequestedPublisher {

        private final List<WhatsappTranscriptExtractionRequestedEvent> events = new ArrayList<>();

        @Override
        public void publishRequested(WhatsappTranscriptExtractionRequestedEvent event) {
            events.add(event);
        }
    }

    static final class CapturingWhatsappPayloadValidationRequestedPublisher implements WhatsappPayloadValidationRequestedPublisher {

        private final List<WhatsappPayloadValidationRequestedEvent> events = new ArrayList<>();

        @Override
        public void publishRequested(WhatsappPayloadValidationRequestedEvent event) {
            events.add(event);
        }
    }

    static final class CapturingWhatsappOutboundDeliveryRequestedPublisher implements WhatsappOutboundDeliveryRequestedPublisher {

        private final List<WhatsappOutboundDeliveryRequestedEvent> events = new ArrayList<>();

        @Override
        public void publishRequested(WhatsappOutboundDeliveryRequestedEvent event) {
            events.add(event);
        }
    }

    static final class CapturingWhatsappButtonConfirmationRequestedPublisher implements WhatsappButtonConfirmationRequestedPublisher {

        private final List<WhatsappButtonConfirmationRequestedEvent> events = new ArrayList<>();

        @Override
        public void publishRequested(WhatsappButtonConfirmationRequestedEvent event) {
            events.add(event);
        }
    }

    static final class FakeWhatsappAudioDownloadGateway implements WhatsappAudioDownloadGateway {

        private final Map<String, DownloadedWhatsappAudio> responses = new LinkedHashMap<>();

        @Override
        public DownloadedWhatsappAudio download(WhatsappAudioPayload payload) {
            return responses.getOrDefault(
                    payload.mediaUrl(),
                    new DownloadedWhatsappAudio(payload.mimeType(), "downloaded-audio".getBytes(StandardCharsets.UTF_8))
            );
        }
    }

    static final class FakeWhatsappAudioStorage implements WhatsappAudioStorage {

        private final Map<String, byte[]> files = new LinkedHashMap<>();

        @Override
        public String store(StoreWhatsappAudioRequest request) {
            String storagePath = "whatsapp/audio/" + request.storageKey() + extensionFor(request.mimeType());
            files.put(storagePath, request.content());
            return storagePath;
        }

        @Override
        public byte[] load(String storagePath) {
            byte[] content = files.get(storagePath);
            if (content == null) {
                throw new IllegalStateException("Missing stored audio for " + storagePath + ".");
            }
            return content;
        }

        private String extensionFor(String mimeType) {
            if (mimeType == null || mimeType.isBlank()) {
                return ".bin";
            }
            return switch (mimeType) {
                case "audio/ogg", "audio/ogg; codecs=opus" -> ".ogg";
                case "audio/mpeg" -> ".mp3";
                default -> ".bin";
            };
        }
    }

    static final class FakeWhatsappAudioTranscoder implements WhatsappAudioTranscoder {

        private RuntimeException failure;

        @Override
        public TranscodedWhatsappAudio transcode(WhatsappAudioTranscodeRequest request) {
            if (failure != null) {
                throw failure;
            }
            return new TranscodedWhatsappAudio("audio/mpeg", (request.providerMessageId() + "-mp3").getBytes(StandardCharsets.UTF_8));
        }
    }

    static final class FakeWhatsappAudioTranscriber implements WhatsappAudioTranscriber {

        private final Map<String, WhatsappAudioTranscriptionResult> results = new LinkedHashMap<>();
        private RuntimeException failure;

        @Override
        public WhatsappAudioTranscriptionResult transcribe(WhatsappAudioTranscriptionRequest request) {
            if (failure != null) {
                throw failure;
            }
            return results.getOrDefault(
                    request.providerMessageId(),
                    new WhatsappAudioTranscriptionResult("openai", "gpt-4o-mini-transcribe", "Transcribed audio")
            );
        }
    }

    static final class FakeWhatsappTranscriptExtractor implements WhatsappTranscriptExtractor {

        private final Map<String, WhatsappTranscriptExtractionResult> results = new LinkedHashMap<>();

        @Override
        public WhatsappTranscriptExtractionResult extract(br.com.curralzap.backend.whatsapp.application.WhatsappTranscriptExtractionRequest request) {
            return results.getOrDefault(
                    request.providerMessageId(),
                    WhatsappTranscriptExtractionResult.unknown("openai", "gpt-5.4-mini", "No fixture configured.", 0.2)
            );
        }
    }

    static final class FakeWhatsappOutboundGateway implements WhatsappOutboundGateway {

        private final List<WhatsappTextOutboundMessage> sentMessages = new ArrayList<>();
        private final List<WhatsappButtonsOutboundMessage> sentButtonMessages = new ArrayList<>();
        private RuntimeException failure;

        @Override
        public WhatsappOutboundDeliveryResult sendText(WhatsappTextOutboundMessage message) {
            if (failure != null) {
                throw failure;
            }
            sentMessages.add(message);
            return new WhatsappOutboundDeliveryResult("outbound-" + sentMessages.size());
        }

        @Override
        public WhatsappOutboundDeliveryResult sendButtons(WhatsappButtonsOutboundMessage message) {
            if (failure != null) {
                throw failure;
            }
            sentButtonMessages.add(message);
            return new WhatsappOutboundDeliveryResult("outbound-buttons-" + sentButtonMessages.size());
        }
    }
}
