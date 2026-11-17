package br.com.curralzap.backend.whatsapp.api;

import br.com.curralzap.backend.support.BackendApiIntegrationTestSupport;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserRole;
import br.com.curralzap.backend.whatsapp.application.WhatsappAudioTranscodeRequestedEvent;
import br.com.curralzap.backend.whatsapp.application.WhatsappAudioTranscodeRequestedPublisher;
import br.com.curralzap.backend.whatsapp.application.WhatsappAudioTranscriptionRequestedEvent;
import br.com.curralzap.backend.whatsapp.application.WhatsappAudioTranscriptionRequestedPublisher;
import br.com.curralzap.backend.whatsapp.application.WhatsappOutboundDeliveryRequestedEvent;
import br.com.curralzap.backend.whatsapp.application.WhatsappOutboundDeliveryRequestedPublisher;
import br.com.curralzap.backend.whatsapp.application.WhatsappReceivedMessagePublisher;
import br.com.curralzap.backend.whatsapp.application.WhatsappMessageReceivedEvent;
import br.com.curralzap.backend.whatsapp.application.WhatsappTranscriptExtractionRequestedEvent;
import br.com.curralzap.backend.whatsapp.application.WhatsappTranscriptExtractionRequestedPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(WhatsappSupportApiIntegrationTest.TestPublishersConfiguration.class)
class WhatsappSupportApiIntegrationTest extends BackendApiIntegrationTestSupport {

    @Autowired
    private CapturingWhatsappReceivedMessagePublisher receivedPublisher;

    @Autowired
    private CapturingWhatsappAudioTranscodeRequestedPublisher audioTranscodePublisher;

    @Autowired
    private CapturingWhatsappAudioTranscriptionRequestedPublisher audioTranscriptionPublisher;

    @Autowired
    private CapturingWhatsappTranscriptExtractionRequestedPublisher transcriptExtractionPublisher;

    @Autowired
    private CapturingWhatsappOutboundDeliveryRequestedPublisher outboundDeliveryPublisher;

    @BeforeEach
    void resetPublishers() {
        receivedPublisher.events.clear();
        audioTranscodePublisher.events.clear();
        audioTranscriptionPublisher.events.clear();
        transcriptExtractionPublisher.events.clear();
        outboundDeliveryPublisher.events.clear();
    }

    @Test
    void adminCanListFailedMessagesWithSearchAndStatusFilters() throws Exception {
        UUID farmId = insertFarm("Fazenda Support");
        UserAccount admin = insertUser(farmId, "5511999998001", "Admin Support", UserRole.ADMIN, true);

        UUID matchingMessageId = insertWhatsappInboundMessage(
                "wamid.search.match",
                admin.phone(),
                farmId,
                admin.id(),
                "req-support-search-1",
                "messages.upsert",
                "audioMessage",
                "FAILED",
                "Audio transcode failed: Unsupported input codec."
        );
        insertProcessingStep(
                matchingMessageId,
                "FAILED",
                "Audio transcode failed: Unsupported input codec.",
                OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1)
        );

        UUID otherMessageId = insertWhatsappInboundMessage(
                "wamid.search.other",
                admin.phone(),
                farmId,
                admin.id(),
                "req-support-search-2",
                "messages.upsert",
                "conversation",
                "REJECTED",
                "Validation rejected unsupported intent: unknown."
        );
        insertProcessingStep(
                otherMessageId,
                "REJECTED",
                "Validation rejected unsupported intent: unknown.",
                OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(2)
        );

        String accessToken = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/whatsapp/messages")
                        .param("status", "failed")
                        .param("search", "match")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value(matchingMessageId.toString()))
                .andExpect(jsonPath("$.items[0].providerMessageId").value("wamid.search.match"))
                .andExpect(jsonPath("$.items[0].status").value("failed"));
    }

    @Test
    void nonAdminCannotAccessWhatsappSupportEndpoints() throws Exception {
        UUID farmId = insertFarm("Fazenda Worker Support");
        UserAccount worker = insertUser(farmId, "5511999998002", "Worker Support", UserRole.WORKER, true);
        String accessToken = issueAccessToken(worker, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/whatsapp/messages")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
    }

    @Test
    void adminCanInspectMessageDetailWithRetryMetadata() throws Exception {
        UUID farmId = insertFarm("Fazenda Detail Support");
        UserAccount admin = insertUser(farmId, "5511999998003", "Admin Detail", UserRole.ADMIN, true);
        UUID inboundMessageId = insertWhatsappInboundMessage(
                "wamid.detail.support",
                admin.phone(),
                farmId,
                admin.id(),
                "req-support-detail",
                "messages.upsert",
                "audioMessage",
                "REJECTED",
                "Validation rejected missing animal 347."
        );
        insertProcessingStep(
                inboundMessageId,
                "REJECTED",
                "Validation rejected missing animal 347.",
                OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1)
        );
        insertAudioMedia(inboundMessageId);
        insertTranscript(inboundMessageId, "Peso 520 quilos");
        insertExtractedPayload(inboundMessageId, "register_weighing", "{\"intent\":\"register_weighing\"}");
        insertOutboundMessage(
                inboundMessageId,
                farmId,
                admin.id(),
                "req-support-detail",
                admin.phone(),
                "text",
                "{\"text\":\"Confirma?\"}",
                "FAILED",
                "Gateway timeout."
        );

        String accessToken = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/whatsapp/messages/{messageId}", inboundMessageId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerMessageId").value("wamid.detail.support"))
                .andExpect(jsonPath("$.retry.supported").value(true))
                .andExpect(jsonPath("$.retry.action").value("outbound_delivery_requested"))
                .andExpect(jsonPath("$.processingSteps[0].status").value("rejected"))
                .andExpect(jsonPath("$.transcript.transcript").value("Peso 520 quilos"))
                .andExpect(jsonPath("$.extractedPayload.intent").value("register_weighing"))
                .andExpect(jsonPath("$.outboundMessages[0].status").value("failed"));
    }

    @Test
    void retryEndpointRepublishesTheSupportedWorkerEvent() throws Exception {
        UUID farmId = insertFarm("Fazenda Retry Support");
        UserAccount admin = insertUser(farmId, "5511999998004", "Admin Retry", UserRole.ADMIN, true);
        UUID inboundMessageId = insertWhatsappInboundMessage(
                "wamid.retry.support",
                admin.phone(),
                farmId,
                admin.id(),
                "req-support-retry",
                "messages.upsert",
                "audioMessage",
                "FAILED",
                "Audio transcode failed: Unsupported input codec."
        );
        insertProcessingStep(
                inboundMessageId,
                "FAILED",
                "Audio transcode failed: Unsupported input codec.",
                OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1)
        );
        insertAudioMedia(inboundMessageId);

        String accessToken = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(post("/api/v1/whatsapp/messages/{messageId}/retry", inboundMessageId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("accepted"))
                .andExpect(jsonPath("$.action").value("audio_transcode_requested"));

        assertThat(audioTranscodePublisher.events).singleElement()
                .extracting(WhatsappAudioTranscodeRequestedEvent::inboundMessageId)
                .isEqualTo(inboundMessageId);
    }

    @Test
    void retryEndpointRejectsUnsupportedBusinessRejections() throws Exception {
        UUID farmId = insertFarm("Fazenda Retry Reject");
        UserAccount admin = insertUser(farmId, "5511999998005", "Admin Reject", UserRole.ADMIN, true);
        UUID inboundMessageId = insertWhatsappInboundMessage(
                "wamid.retry.reject",
                admin.phone(),
                farmId,
                admin.id(),
                "req-support-reject",
                "messages.upsert",
                "conversation",
                "REJECTED",
                "Validation rejected unsupported intent: unknown."
        );
        insertProcessingStep(
                inboundMessageId,
                "REJECTED",
                "Validation rejected unsupported intent: unknown.",
                OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1)
        );

        String accessToken = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(post("/api/v1/whatsapp/messages/{messageId}/retry", inboundMessageId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("invalid_state_transition"));
    }

    private void insertProcessingStep(
            UUID inboundMessageId,
            String status,
            String details,
            OffsetDateTime occurredAt
    ) {
        jdbcTemplate.update(
                """
                        insert into whatsapp_processing_steps (
                            id, inbound_message_id, status, details, occurred_at, created_at, updated_at
                        ) values (?, ?, ?, ?, ?, ?, ?)
                        """,
                UUID.randomUUID(),
                inboundMessageId,
                status,
                details,
                Timestamp.from(occurredAt.toInstant()),
                Timestamp.from(occurredAt.toInstant()),
                Timestamp.from(occurredAt.toInstant())
        );
    }

    private void insertAudioMedia(UUID inboundMessageId) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update(
                """
                        insert into whatsapp_audio_media (
                            id, inbound_message_id, source_url, mime_type, storage_path, downloaded_at,
                            transcoded_mime_type, transcoded_storage_path, transcoded_at, created_at, updated_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                UUID.randomUUID(),
                inboundMessageId,
                "https://evolution.test/media/audio.ogg",
                "audio/ogg",
                "whatsapp/inbound/audio.ogg",
                Timestamp.from(now.minusMinutes(5).toInstant()),
                "audio/mpeg",
                "whatsapp/inbound/audio.mp3",
                Timestamp.from(now.minusMinutes(4).toInstant()),
                Timestamp.from(now.minusMinutes(5).toInstant()),
                Timestamp.from(now.minusMinutes(4).toInstant())
        );
    }

    private void insertTranscript(UUID inboundMessageId, String transcript) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update(
                """
                        insert into whatsapp_transcripts (
                            id, inbound_message_id, provider, provider_model, transcript, transcribed_at, created_at, updated_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                UUID.randomUUID(),
                inboundMessageId,
                "openai",
                "gpt-5.4-mini",
                transcript,
                Timestamp.from(now.minusMinutes(3).toInstant()),
                Timestamp.from(now.minusMinutes(3).toInstant()),
                Timestamp.from(now.minusMinutes(3).toInstant())
        );
    }

    private void insertExtractedPayload(UUID inboundMessageId, String intent, String structuredPayload) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update(
                """
                        insert into whatsapp_extracted_payloads (
                            id, inbound_message_id, provider, provider_model, intent, structured_payload,
                            extracted_at, created_at, updated_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                UUID.randomUUID(),
                inboundMessageId,
                "openai",
                "gpt-5.4-mini",
                intent,
                structuredPayload,
                Timestamp.from(now.minusMinutes(2).toInstant()),
                Timestamp.from(now.minusMinutes(2).toInstant()),
                Timestamp.from(now.minusMinutes(2).toInstant())
        );
    }

    private void insertOutboundMessage(
            UUID inboundMessageId,
            UUID farmId,
            UUID userId,
            String correlationId,
            String phoneNumber,
            String messageType,
            String payload,
            String status,
            String failureReason
    ) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update(
                """
                        insert into whatsapp_outbound_messages (
                            id, inbound_message_id, farm_id, user_id, correlation_id, phone_number, message_type,
                            payload, provider_message_id, status, failure_reason, queued_at, sent_at, created_at, updated_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                UUID.randomUUID(),
                inboundMessageId,
                farmId,
                userId,
                correlationId,
                phoneNumber,
                messageType,
                payload,
                null,
                status,
                failureReason,
                Timestamp.from(now.minusMinutes(1).toInstant()),
                null,
                Timestamp.from(now.minusMinutes(1).toInstant()),
                Timestamp.from(now.minusMinutes(1).toInstant())
        );
    }

    @TestConfiguration
    static class TestPublishersConfiguration {

        @Bean
        @Primary
        CapturingWhatsappReceivedMessagePublisher receivedPublisher() {
            return new CapturingWhatsappReceivedMessagePublisher();
        }

        @Bean
        @Primary
        CapturingWhatsappAudioTranscodeRequestedPublisher audioTranscodePublisher() {
            return new CapturingWhatsappAudioTranscodeRequestedPublisher();
        }

        @Bean
        @Primary
        CapturingWhatsappAudioTranscriptionRequestedPublisher audioTranscriptionPublisher() {
            return new CapturingWhatsappAudioTranscriptionRequestedPublisher();
        }

        @Bean
        @Primary
        CapturingWhatsappTranscriptExtractionRequestedPublisher transcriptExtractionPublisher() {
            return new CapturingWhatsappTranscriptExtractionRequestedPublisher();
        }

        @Bean
        @Primary
        CapturingWhatsappOutboundDeliveryRequestedPublisher outboundDeliveryPublisher() {
            return new CapturingWhatsappOutboundDeliveryRequestedPublisher();
        }
    }

    static class CapturingWhatsappReceivedMessagePublisher implements WhatsappReceivedMessagePublisher {

        private final List<WhatsappMessageReceivedEvent> events = new ArrayList<>();

        @Override
        public void publishReceived(WhatsappMessageReceivedEvent event) {
            events.add(event);
        }
    }

    static class CapturingWhatsappAudioTranscodeRequestedPublisher implements WhatsappAudioTranscodeRequestedPublisher {

        private final List<WhatsappAudioTranscodeRequestedEvent> events = new ArrayList<>();

        @Override
        public void publishRequested(WhatsappAudioTranscodeRequestedEvent event) {
            events.add(event);
        }
    }

    static class CapturingWhatsappAudioTranscriptionRequestedPublisher
            implements WhatsappAudioTranscriptionRequestedPublisher {

        private final List<WhatsappAudioTranscriptionRequestedEvent> events = new ArrayList<>();

        @Override
        public void publishRequested(WhatsappAudioTranscriptionRequestedEvent event) {
            events.add(event);
        }
    }

    static class CapturingWhatsappTranscriptExtractionRequestedPublisher
            implements WhatsappTranscriptExtractionRequestedPublisher {

        private final List<WhatsappTranscriptExtractionRequestedEvent> events = new ArrayList<>();

        @Override
        public void publishRequested(WhatsappTranscriptExtractionRequestedEvent event) {
            events.add(event);
        }
    }

    static class CapturingWhatsappOutboundDeliveryRequestedPublisher
            implements WhatsappOutboundDeliveryRequestedPublisher {

        private final List<WhatsappOutboundDeliveryRequestedEvent> events = new ArrayList<>();

        @Override
        public void publishRequested(WhatsappOutboundDeliveryRequestedEvent event) {
            events.add(event);
        }
    }
}
