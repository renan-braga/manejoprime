package br.com.curralzap.backend.whatsapp.application;

import br.com.curralzap.backend.whatsapp.domain.WhatsappAudioMedia;
import br.com.curralzap.backend.whatsapp.domain.WhatsappInboundMessage;
import br.com.curralzap.backend.whatsapp.domain.WhatsappProcessingStatus;
import br.com.curralzap.backend.whatsapp.domain.WhatsappProcessingStep;
import br.com.curralzap.backend.whatsapp.domain.WhatsappTranscript;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WhatsappAudioDownloadServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-04-04T10:15:30Z"), ZoneOffset.UTC);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void downloadsAudioPersistsMediaReferenceAdvancesProcessingStateAndPublishesTranscodeRequest() {
        InMemoryWhatsappInboundMessageRepository inboundMessages = new InMemoryWhatsappInboundMessageRepository();
        InMemoryWhatsappAudioMediaRepository audioMediaRepository = new InMemoryWhatsappAudioMediaRepository();
        CapturingWhatsappAudioDownloadGateway downloadGateway = new CapturingWhatsappAudioDownloadGateway();
        CapturingWhatsappAudioStorage storage = new CapturingWhatsappAudioStorage();
        CapturingWhatsappAudioTranscodeRequestedPublisher publisher = new CapturingWhatsappAudioTranscodeRequestedPublisher();
        WhatsappAudioDownloadService service = new WhatsappAudioDownloadService(
                inboundMessages,
                audioMediaRepository,
                new FixedWhatsappAudioPayloadExtractor("https://media.example/audio-1.ogg", "audio/ogg"),
                downloadGateway,
                storage,
                publisher,
                new InMemoryWhatsappTranscriptRepository(),
                new CapturingWhatsappTranscriptExtractionRequestedPublisher(),
                OBJECT_MAPPER,
                FIXED_CLOCK
        );
        WhatsappInboundMessage inboundMessage = inboundMessages.save(WhatsappInboundMessage.receive(
                "wamid.audio-1",
                "messages.upsert",
                "5511999990001",
                null,
                null,
                "audioMessage",
                "{\"event\":\"messages.upsert\"}",
                OffsetDateTime.parse("2026-04-04T10:10:00Z")
        ));

        service.handleReceivedMessage(new WhatsappMessageReceivedEvent(
                1,
                "whatsapp.message.received.v1",
                inboundMessage.id().toString(),
                "req-wa-005-download-ok",
                inboundMessage.receivedAt(),
                inboundMessage.id(),
                inboundMessage.providerMessageId(),
                inboundMessage.farmId(),
                inboundMessage.userId(),
                inboundMessage.phoneNumber(),
                inboundMessage.messageType(),
                inboundMessage.status()
        ));

        assertThat(downloadGateway.urls).containsExactly("https://media.example/audio-1.ogg");
        assertThat(storage.requests).singleElement().satisfies(request -> {
            assertThat(request.inboundMessageId()).isEqualTo(inboundMessage.id());
            assertThat(request.providerMessageId()).isEqualTo("wamid.audio-1");
            assertThat(request.storageKey()).isEqualTo("wamid.audio-1");
            assertThat(request.mimeType()).isEqualTo("audio/ogg");
        });
        assertThat(audioMediaRepository.findByInboundMessageId(inboundMessage.id()))
                .get()
                .satisfies(media -> {
                    assertThat(media.storagePath()).isEqualTo("whatsapp/audio/wamid.audio-1.ogg");
                    assertThat(media.mimeType()).isEqualTo("audio/ogg");
                    assertThat(media.sourceUrl()).isEqualTo("https://media.example/audio-1.ogg");
                    assertThat(media.downloadedAt()).isEqualTo(OffsetDateTime.now(FIXED_CLOCK));
                    assertThat(media.transcodedAt()).isNull();
                });
        assertThat(publisher.events).singleElement().satisfies(event -> {
            assertThat(event.providerMessageId()).isEqualTo("wamid.audio-1");
            assertThat(event.sourceStoragePath()).isEqualTo("whatsapp/audio/wamid.audio-1.ogg");
            assertThat(event.sourceMimeType()).isEqualTo("audio/ogg");
        });
        assertThat(inboundMessages.findByProviderMessageId("wamid.audio-1"))
                .get()
                .extracting(WhatsappInboundMessage::status)
                .isEqualTo(WhatsappProcessingStatus.MEDIA_DOWNLOADED);
        assertThat(inboundMessages.findProcessingSteps(inboundMessage.id()))
                .extracting(WhatsappProcessingStep::status, WhatsappProcessingStep::details)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(WhatsappProcessingStatus.RECEIVED, null),
                        org.assertj.core.groups.Tuple.tuple(
                                WhatsappProcessingStatus.MEDIA_DOWNLOADED,
                                "Audio downloaded to whatsapp/audio/wamid.audio-1.ogg."
                        )
                );
    }

    @Test
    void republishesTranscodeRequestWhenDownloadedAudioEventIsRetriedBeforeTranscode() {
        InMemoryWhatsappInboundMessageRepository inboundMessages = new InMemoryWhatsappInboundMessageRepository();
        InMemoryWhatsappAudioMediaRepository audioMediaRepository = new InMemoryWhatsappAudioMediaRepository();
        CapturingWhatsappAudioDownloadGateway downloadGateway = new CapturingWhatsappAudioDownloadGateway();
        CapturingWhatsappAudioStorage storage = new CapturingWhatsappAudioStorage();
        CapturingWhatsappAudioTranscodeRequestedPublisher publisher = new CapturingWhatsappAudioTranscodeRequestedPublisher();
        WhatsappAudioDownloadService service = new WhatsappAudioDownloadService(
                inboundMessages,
                audioMediaRepository,
                new FixedWhatsappAudioPayloadExtractor("https://media.example/audio-2.ogg", "audio/ogg"),
                downloadGateway,
                storage,
                publisher,
                new InMemoryWhatsappTranscriptRepository(),
                new CapturingWhatsappTranscriptExtractionRequestedPublisher(),
                OBJECT_MAPPER,
                FIXED_CLOCK
        );
        WhatsappInboundMessage inboundMessage = inboundMessages.save(WhatsappInboundMessage.receive(
                "wamid.audio-2",
                "messages.upsert",
                "5511999990002",
                null,
                null,
                "audioMessage",
                "{\"event\":\"messages.upsert\"}",
                OffsetDateTime.parse("2026-04-04T10:11:00Z")
        ));
        audioMediaRepository.save(WhatsappAudioMedia.downloaded(
                inboundMessage.id(),
                "https://media.example/audio-2.ogg",
                "audio/ogg",
                "whatsapp/audio/existing.ogg",
                OffsetDateTime.parse("2026-04-04T10:12:00Z")
        ));
        inboundMessages.recordProcessingStep(
                inboundMessage.id(),
                WhatsappProcessingStatus.MEDIA_DOWNLOADED,
                "Audio downloaded to whatsapp/audio/existing.ogg.",
                OffsetDateTime.parse("2026-04-04T10:12:00Z")
        );

        service.handleReceivedMessage(new WhatsappMessageReceivedEvent(
                1,
                "whatsapp.message.received.v1",
                inboundMessage.id().toString(),
                "req-wa-005-download-retry",
                inboundMessage.receivedAt(),
                inboundMessage.id(),
                inboundMessage.providerMessageId(),
                inboundMessage.farmId(),
                inboundMessage.userId(),
                inboundMessage.phoneNumber(),
                inboundMessage.messageType(),
                WhatsappProcessingStatus.MEDIA_DOWNLOADED
        ));

        assertThat(downloadGateway.urls).isEmpty();
        assertThat(storage.requests).isEmpty();
        assertThat(publisher.events).singleElement().satisfies(event ->
                assertThat(event.sourceStoragePath()).isEqualTo("whatsapp/audio/existing.ogg"));
        assertThat(inboundMessages.findProcessingSteps(inboundMessage.id())).hasSize(2);
    }

    @Test
    void recordsFailedStateWhenAudioPayloadIsMissingMediaUrl() {
        InMemoryWhatsappInboundMessageRepository inboundMessages = new InMemoryWhatsappInboundMessageRepository();
        WhatsappAudioDownloadService service = new WhatsappAudioDownloadService(
                inboundMessages,
                new InMemoryWhatsappAudioMediaRepository(),
                new MissingWhatsappAudioPayloadExtractor(),
                new CapturingWhatsappAudioDownloadGateway(),
                new CapturingWhatsappAudioStorage(),
                new CapturingWhatsappAudioTranscodeRequestedPublisher(),
                new InMemoryWhatsappTranscriptRepository(),
                new CapturingWhatsappTranscriptExtractionRequestedPublisher(),
                OBJECT_MAPPER,
                FIXED_CLOCK
        );
        WhatsappInboundMessage inboundMessage = inboundMessages.save(WhatsappInboundMessage.receive(
                "wamid.audio-missing",
                "messages.upsert",
                "5511999990003",
                null,
                null,
                "audioMessage",
                "{\"event\":\"messages.upsert\"}",
                OffsetDateTime.parse("2026-04-04T10:13:00Z")
        ));

        service.handleReceivedMessage(new WhatsappMessageReceivedEvent(
                1,
                "whatsapp.message.received.v1",
                inboundMessage.id().toString(),
                "req-wa-005-download-missing",
                inboundMessage.receivedAt(),
                inboundMessage.id(),
                inboundMessage.providerMessageId(),
                inboundMessage.farmId(),
                inboundMessage.userId(),
                inboundMessage.phoneNumber(),
                inboundMessage.messageType(),
                inboundMessage.status()
        ));

        assertThat(inboundMessages.findByProviderMessageId("wamid.audio-missing"))
                .get()
                .extracting(WhatsappInboundMessage::status)
                .isEqualTo(WhatsappProcessingStatus.FAILED);
        assertThat(inboundMessages.findProcessingSteps(inboundMessage.id()))
                .last()
                .satisfies(step -> {
                    assertThat(step.status()).isEqualTo(WhatsappProcessingStatus.FAILED);
                    assertThat(step.details()).isEqualTo("Audio payload has no downloadable URL or inline content.");
                });
    }

    @Test
    void throwsRetryableFailureWhenAudioDownloadFails() {
        InMemoryWhatsappInboundMessageRepository inboundMessages = new InMemoryWhatsappInboundMessageRepository();
        CapturingWhatsappAudioDownloadGateway downloadGateway = new CapturingWhatsappAudioDownloadGateway();
        downloadGateway.failure = new IllegalStateException("Evolution media download unavailable.");
        CapturingWhatsappAudioStorage storage = new CapturingWhatsappAudioStorage();
        CapturingWhatsappAudioTranscodeRequestedPublisher publisher = new CapturingWhatsappAudioTranscodeRequestedPublisher();
        WhatsappAudioDownloadService service = new WhatsappAudioDownloadService(
                inboundMessages,
                new InMemoryWhatsappAudioMediaRepository(),
                new FixedWhatsappAudioPayloadExtractor("https://media.example/audio-5.ogg", "audio/ogg"),
                downloadGateway,
                storage,
                publisher,
                new InMemoryWhatsappTranscriptRepository(),
                new CapturingWhatsappTranscriptExtractionRequestedPublisher(),
                OBJECT_MAPPER,
                FIXED_CLOCK
        );
        WhatsappInboundMessage inboundMessage = inboundMessages.save(WhatsappInboundMessage.receive(
                "wamid.audio-download-fail",
                "messages.upsert",
                "5511999990005",
                null,
                null,
                "audioMessage",
                "{\"event\":\"messages.upsert\"}",
                OffsetDateTime.parse("2026-04-04T10:15:00Z")
        ));

        assertThatThrownBy(() -> service.handleReceivedMessage(new WhatsappMessageReceivedEvent(
                1,
                "whatsapp.message.received.v1",
                inboundMessage.id().toString(),
                "req-wa-011-download-fail",
                inboundMessage.receivedAt(),
                inboundMessage.id(),
                inboundMessage.providerMessageId(),
                inboundMessage.farmId(),
                inboundMessage.userId(),
                inboundMessage.phoneNumber(),
                inboundMessage.messageType(),
                inboundMessage.status()
        )))
                .isInstanceOf(WhatsappWorkerRetryableException.class)
                .hasMessage("Failed to process WhatsApp audio download worker event.");

        assertThat(inboundMessages.findByProviderMessageId("wamid.audio-download-fail"))
                .get()
                .extracting(WhatsappInboundMessage::status)
                .isEqualTo(WhatsappProcessingStatus.FAILED);
        assertThat(inboundMessages.findProcessingSteps(inboundMessage.id()))
                .last()
                .satisfies(step -> {
                    assertThat(step.status()).isEqualTo(WhatsappProcessingStatus.FAILED);
                    assertThat(step.details())
                            .isEqualTo("Audio download failed: Evolution media download unavailable.");
                });
        assertThat(storage.requests).isEmpty();
        assertThat(publisher.events).isEmpty();
    }

    @Test
    void ignoresUnsupportedMessageTypes() {
        InMemoryWhatsappInboundMessageRepository inboundMessages = new InMemoryWhatsappInboundMessageRepository();
        CapturingWhatsappAudioDownloadGateway downloadGateway = new CapturingWhatsappAudioDownloadGateway();
        CapturingWhatsappAudioStorage storage = new CapturingWhatsappAudioStorage();
        CapturingWhatsappAudioTranscodeRequestedPublisher transcodePublisher = new CapturingWhatsappAudioTranscodeRequestedPublisher();
        CapturingWhatsappTranscriptExtractionRequestedPublisher extractionPublisher = new CapturingWhatsappTranscriptExtractionRequestedPublisher();
        WhatsappAudioDownloadService service = new WhatsappAudioDownloadService(
                inboundMessages,
                new InMemoryWhatsappAudioMediaRepository(),
                new FixedWhatsappAudioPayloadExtractor("https://media.example/audio-4.ogg", "audio/ogg"),
                downloadGateway,
                storage,
                transcodePublisher,
                new InMemoryWhatsappTranscriptRepository(),
                extractionPublisher,
                OBJECT_MAPPER,
                FIXED_CLOCK
        );
        WhatsappInboundMessage inboundMessage = inboundMessages.save(WhatsappInboundMessage.receive(
                "wamid.image-1",
                "messages.upsert",
                "5511999990004",
                null,
                null,
                "imageMessage",
                "{\"event\":\"messages.upsert\"}",
                OffsetDateTime.parse("2026-04-04T10:14:00Z")
        ));

        service.handleReceivedMessage(new WhatsappMessageReceivedEvent(
                1,
                "whatsapp.message.received.v1",
                inboundMessage.id().toString(),
                "req-wa-005-download-image",
                inboundMessage.receivedAt(),
                inboundMessage.id(),
                inboundMessage.providerMessageId(),
                inboundMessage.farmId(),
                inboundMessage.userId(),
                inboundMessage.phoneNumber(),
                inboundMessage.messageType(),
                inboundMessage.status()
        ));

        assertThat(downloadGateway.urls).isEmpty();
        assertThat(storage.requests).isEmpty();
        assertThat(transcodePublisher.events).isEmpty();
        assertThat(extractionPublisher.events).isEmpty();
        assertThat(inboundMessages.findByProviderMessageId("wamid.image-1"))
                .get()
                .extracting(WhatsappInboundMessage::status)
                .isEqualTo(WhatsappProcessingStatus.RECEIVED);
    }

    @Test
    void routesTextMessageDirectlyToTranscriptExtractionPipeline() {
        InMemoryWhatsappInboundMessageRepository inboundMessages = new InMemoryWhatsappInboundMessageRepository();
        InMemoryWhatsappTranscriptRepository transcriptRepository = new InMemoryWhatsappTranscriptRepository();
        CapturingWhatsappTranscriptExtractionRequestedPublisher extractionPublisher = new CapturingWhatsappTranscriptExtractionRequestedPublisher();
        WhatsappAudioDownloadService service = new WhatsappAudioDownloadService(
                inboundMessages,
                new InMemoryWhatsappAudioMediaRepository(),
                new MissingWhatsappAudioPayloadExtractor(),
                new CapturingWhatsappAudioDownloadGateway(),
                new CapturingWhatsappAudioStorage(),
                new CapturingWhatsappAudioTranscodeRequestedPublisher(),
                transcriptRepository,
                extractionPublisher,
                OBJECT_MAPPER,
                FIXED_CLOCK
        );
        String rawPayload = "{\"event\":\"messages.upsert\",\"data\":{\"message\":{\"conversation\":\"pesagem boi brinco E2E-123 quatrocentos e oitenta quilos\"}}}";
        WhatsappInboundMessage inboundMessage = inboundMessages.save(WhatsappInboundMessage.receive(
                "wamid.text-pesagem",
                "messages.upsert",
                "5511999990010",
                null,
                null,
                "conversation",
                rawPayload,
                OffsetDateTime.parse("2026-04-04T10:17:00Z")
        ));

        service.handleReceivedMessage(new WhatsappMessageReceivedEvent(
                1,
                "whatsapp.message.received.v1",
                inboundMessage.id().toString(),
                "req-wa-013-text",
                inboundMessage.receivedAt(),
                inboundMessage.id(),
                inboundMessage.providerMessageId(),
                inboundMessage.farmId(),
                inboundMessage.userId(),
                inboundMessage.phoneNumber(),
                inboundMessage.messageType(),
                inboundMessage.status()
        ));

        assertThat(transcriptRepository.findByInboundMessageId(inboundMessage.id()))
                .get()
                .satisfies(transcript -> {
                    assertThat(transcript.transcript()).isEqualTo("pesagem boi brinco E2E-123 quatrocentos e oitenta quilos");
                    assertThat(transcript.provider()).isEqualTo("text");
                    assertThat(transcript.providerModel()).isEqualTo("text");
                });
        assertThat(inboundMessages.findByProviderMessageId("wamid.text-pesagem"))
                .get()
                .extracting(WhatsappInboundMessage::status)
                .isEqualTo(WhatsappProcessingStatus.TRANSCRIBED);
        assertThat(extractionPublisher.events).singleElement().satisfies(event ->
                assertThat(event.providerMessageId()).isEqualTo("wamid.text-pesagem"));
    }

    @Test
    void recordsFailedStateWhenTextMessageHasNoConversationContent() {
        InMemoryWhatsappInboundMessageRepository inboundMessages = new InMemoryWhatsappInboundMessageRepository();
        WhatsappAudioDownloadService service = new WhatsappAudioDownloadService(
                inboundMessages,
                new InMemoryWhatsappAudioMediaRepository(),
                new MissingWhatsappAudioPayloadExtractor(),
                new CapturingWhatsappAudioDownloadGateway(),
                new CapturingWhatsappAudioStorage(),
                new CapturingWhatsappAudioTranscodeRequestedPublisher(),
                new InMemoryWhatsappTranscriptRepository(),
                new CapturingWhatsappTranscriptExtractionRequestedPublisher(),
                OBJECT_MAPPER,
                FIXED_CLOCK
        );
        WhatsappInboundMessage inboundMessage = inboundMessages.save(WhatsappInboundMessage.receive(
                "wamid.text-empty",
                "messages.upsert",
                "5511999990011",
                null,
                null,
                "conversation",
                "{\"event\":\"messages.upsert\"}",
                OffsetDateTime.parse("2026-04-04T10:18:00Z")
        ));

        service.handleReceivedMessage(new WhatsappMessageReceivedEvent(
                1,
                "whatsapp.message.received.v1",
                inboundMessage.id().toString(),
                "req-wa-014-text-empty",
                inboundMessage.receivedAt(),
                inboundMessage.id(),
                inboundMessage.providerMessageId(),
                inboundMessage.farmId(),
                inboundMessage.userId(),
                inboundMessage.phoneNumber(),
                inboundMessage.messageType(),
                inboundMessage.status()
        ));

        assertThat(inboundMessages.findByProviderMessageId("wamid.text-empty"))
                .get()
                .extracting(WhatsappInboundMessage::status)
                .isEqualTo(WhatsappProcessingStatus.FAILED);
    }

    @Test
    void storesInlineBase64AudioWithoutCallingDownloadGateway() {
        InMemoryWhatsappInboundMessageRepository inboundMessages = new InMemoryWhatsappInboundMessageRepository();
        InMemoryWhatsappAudioMediaRepository audioMediaRepository = new InMemoryWhatsappAudioMediaRepository();
        CapturingWhatsappAudioDownloadGateway downloadGateway = new CapturingWhatsappAudioDownloadGateway();
        CapturingWhatsappAudioStorage storage = new CapturingWhatsappAudioStorage();
        CapturingWhatsappAudioTranscodeRequestedPublisher publisher = new CapturingWhatsappAudioTranscodeRequestedPublisher();
        byte[] audioBytes = "fake-ogg-bytes".getBytes();
        WhatsappAudioDownloadService service = new WhatsappAudioDownloadService(
                inboundMessages,
                audioMediaRepository,
                new FixedWhatsappAudioPayloadExtractor(null, "audio/ogg", audioBytes),
                downloadGateway,
                storage,
                publisher,
                new InMemoryWhatsappTranscriptRepository(),
                new CapturingWhatsappTranscriptExtractionRequestedPublisher(),
                OBJECT_MAPPER,
                FIXED_CLOCK
        );
        WhatsappInboundMessage inboundMessage = inboundMessages.save(WhatsappInboundMessage.receive(
                "wamid.audio-base64",
                "messages.upsert",
                "5511900000000",
                null,
                null,
                "audioMessage",
                "{\"event\":\"messages.upsert\"}",
                OffsetDateTime.parse("2026-04-04T10:16:00Z")
        ));

        service.handleReceivedMessage(new WhatsappMessageReceivedEvent(
                1,
                "whatsapp.message.received.v1",
                inboundMessage.id().toString(),
                "req-wa-012-base64",
                inboundMessage.receivedAt(),
                inboundMessage.id(),
                inboundMessage.providerMessageId(),
                inboundMessage.farmId(),
                inboundMessage.userId(),
                inboundMessage.phoneNumber(),
                inboundMessage.messageType(),
                inboundMessage.status()
        ));

        assertThat(downloadGateway.urls).isEmpty();
        assertThat(storage.requests).singleElement().satisfies(request -> {
            assertThat(request.inboundMessageId()).isEqualTo(inboundMessage.id());
            assertThat(request.mimeType()).isEqualTo("audio/ogg");
            assertThat(request.content()).isEqualTo(audioBytes);
        });
        assertThat(audioMediaRepository.findByInboundMessageId(inboundMessage.id()))
                .get()
                .satisfies(media -> {
                    assertThat(media.sourceUrl()).isNull();
                    assertThat(media.mimeType()).isEqualTo("audio/ogg");
                    assertThat(media.storagePath()).isEqualTo("whatsapp/audio/wamid.audio-base64.ogg");
                });
        assertThat(publisher.events).singleElement().satisfies(event ->
                assertThat(event.sourceStoragePath()).isEqualTo("whatsapp/audio/wamid.audio-base64.ogg"));
    }

    private static final class InMemoryWhatsappInboundMessageRepository implements WhatsappInboundMessageRepository {

        private final Map<UUID, WhatsappInboundMessage> messagesById = new LinkedHashMap<>();
        private final Map<UUID, List<WhatsappProcessingStep>> stepsByMessageId = new LinkedHashMap<>();

        @Override
        public WhatsappInboundMessage save(WhatsappInboundMessage message) {
            WhatsappInboundMessage persisted = message.id() == null
                    ? new WhatsappInboundMessage(
                    UUID.randomUUID(),
                    message.providerMessageId(),
                    message.eventType(),
                    message.phoneNumber(),
                    message.farmId(),
                    message.userId(),
                    message.correlationId(),
                    message.messageType(),
                    message.rawPayload(),
                    message.status(),
                    message.failureReason(),
                    message.receivedAt(),
                    message.createdAt(),
                    message.updatedAt()
            )
                    : message;
            messagesById.put(persisted.id(), persisted);
            if (!stepsByMessageId.containsKey(persisted.id())) {
                stepsByMessageId.put(persisted.id(), new ArrayList<>(List.of(new WhatsappProcessingStep(
                        UUID.randomUUID(),
                        persisted.id(),
                        persisted.status(),
                        null,
                        persisted.receivedAt(),
                        persisted.createdAt(),
                        persisted.updatedAt()
                ))));
            }
            return persisted;
        }

        @Override
        public Optional<WhatsappInboundMessage> findByProviderMessageId(String providerMessageId) {
            return messagesById.values().stream()
                    .filter(message -> message.providerMessageId().equals(providerMessageId))
                    .findFirst();
        }

        @Override
        public Optional<WhatsappInboundMessage> findById(UUID inboundMessageId) {
            return Optional.ofNullable(messagesById.get(inboundMessageId));
        }

        @Override
        public List<WhatsappInboundMessage> findRecentByFarmId(UUID farmId, int limit) {
            return messagesById.values().stream()
                    .filter(message -> farmId.equals(message.farmId()))
                    .sorted((left, right) -> right.createdAt().compareTo(left.createdAt()))
                    .limit(limit)
                    .toList();
        }

        @Override
        public List<WhatsappProcessingStep> findProcessingSteps(UUID inboundMessageId) {
            return stepsByMessageId.getOrDefault(inboundMessageId, List.of());
        }

        @Override
        public WhatsappInboundMessage recordProcessingStep(
                UUID inboundMessageId,
                WhatsappProcessingStatus status,
                String details,
                OffsetDateTime occurredAt
        ) {
            WhatsappInboundMessage current = messagesById.get(inboundMessageId);
            WhatsappInboundMessage updated = new WhatsappInboundMessage(
                    current.id(),
                    current.providerMessageId(),
                    current.eventType(),
                    current.phoneNumber(),
                    current.farmId(),
                    current.userId(),
                    current.correlationId(),
                    current.messageType(),
                    current.rawPayload(),
                    status,
                    status == WhatsappProcessingStatus.FAILED ? details : null,
                    current.receivedAt(),
                    current.createdAt(),
                    occurredAt
            );
            messagesById.put(inboundMessageId, updated);
            stepsByMessageId.computeIfAbsent(inboundMessageId, ignored -> new ArrayList<>())
                    .add(new WhatsappProcessingStep(
                            UUID.randomUUID(),
                            inboundMessageId,
                            status,
                            details,
                            occurredAt,
                            occurredAt,
                            occurredAt
                    ));
            return updated;
        }
    }

    private static final class InMemoryWhatsappAudioMediaRepository implements WhatsappAudioMediaRepository {

        private final Map<UUID, WhatsappAudioMedia> mediaByInboundMessageId = new LinkedHashMap<>();

        @Override
        public WhatsappAudioMedia save(WhatsappAudioMedia audioMedia) {
            WhatsappAudioMedia persisted = audioMedia.id() == null
                    ? new WhatsappAudioMedia(
                    UUID.randomUUID(),
                    audioMedia.inboundMessageId(),
                    audioMedia.sourceUrl(),
                    audioMedia.mimeType(),
                    audioMedia.storagePath(),
                    audioMedia.downloadedAt(),
                    audioMedia.transcodedMimeType(),
                    audioMedia.transcodedStoragePath(),
                    audioMedia.transcodedAt(),
                    audioMedia.createdAt(),
                    audioMedia.updatedAt()
            )
                    : audioMedia;
            mediaByInboundMessageId.put(persisted.inboundMessageId(), persisted);
            return persisted;
        }

        @Override
        public Optional<WhatsappAudioMedia> findByInboundMessageId(UUID inboundMessageId) {
            return Optional.ofNullable(mediaByInboundMessageId.get(inboundMessageId));
        }
    }

    private static final class InMemoryWhatsappTranscriptRepository implements WhatsappTranscriptRepository {

        private final Map<UUID, WhatsappTranscript> transcriptsByMessageId = new LinkedHashMap<>();

        @Override
        public WhatsappTranscript save(WhatsappTranscript transcript) {
            WhatsappTranscript persisted = transcript.id() == null
                    ? new WhatsappTranscript(
                    UUID.randomUUID(),
                    transcript.inboundMessageId(),
                    transcript.provider(),
                    transcript.providerModel(),
                    transcript.transcript(),
                    transcript.transcribedAt(),
                    transcript.createdAt(),
                    transcript.updatedAt()
            )
                    : transcript;
            transcriptsByMessageId.put(persisted.inboundMessageId(), persisted);
            return persisted;
        }

        @Override
        public Optional<WhatsappTranscript> findByInboundMessageId(UUID inboundMessageId) {
            return Optional.ofNullable(transcriptsByMessageId.get(inboundMessageId));
        }
    }

    private static final class FixedWhatsappAudioPayloadExtractor implements WhatsappAudioPayloadExtractor {

        private final String mediaUrl;
        private final String mimeType;
        private final byte[] rawContent;

        private FixedWhatsappAudioPayloadExtractor(String mediaUrl, String mimeType) {
            this(mediaUrl, mimeType, null);
        }

        private FixedWhatsappAudioPayloadExtractor(String mediaUrl, String mimeType, byte[] rawContent) {
            this.mediaUrl = mediaUrl;
            this.mimeType = mimeType;
            this.rawContent = rawContent;
        }

        @Override
        public Optional<WhatsappAudioPayload> extract(WhatsappInboundMessage message) {
            return Optional.of(new WhatsappAudioPayload(mediaUrl, mimeType, rawContent));
        }
    }

    private static final class MissingWhatsappAudioPayloadExtractor implements WhatsappAudioPayloadExtractor {

        @Override
        public Optional<WhatsappAudioPayload> extract(WhatsappInboundMessage message) {
            return Optional.empty();
        }
    }

    private static final class CapturingWhatsappAudioDownloadGateway implements WhatsappAudioDownloadGateway {

        private final List<String> urls = new ArrayList<>();
        private RuntimeException failure;

        @Override
        public DownloadedWhatsappAudio download(WhatsappAudioPayload payload) {
            urls.add(payload.mediaUrl());
            if (failure != null) {
                throw failure;
            }
            return new DownloadedWhatsappAudio(
                    "audio/ogg",
                    "voice-note".getBytes(StandardCharsets.UTF_8)
            );
        }
    }

    private static final class CapturingWhatsappAudioStorage implements WhatsappAudioStorage {

        private final List<StoreWhatsappAudioRequest> requests = new ArrayList<>();

        @Override
        public String store(StoreWhatsappAudioRequest request) {
            requests.add(request);
            return "whatsapp/audio/" + request.storageKey() + ".ogg";
        }

        @Override
        public byte[] load(String storagePath) {
            throw new UnsupportedOperationException("Not needed in this test");
        }
    }

    private static final class CapturingWhatsappAudioTranscodeRequestedPublisher
            implements WhatsappAudioTranscodeRequestedPublisher {

        private final List<WhatsappAudioTranscodeRequestedEvent> events = new ArrayList<>();

        @Override
        public void publishRequested(WhatsappAudioTranscodeRequestedEvent event) {
            events.add(event);
        }
    }

    private static final class CapturingWhatsappTranscriptExtractionRequestedPublisher
            implements WhatsappTranscriptExtractionRequestedPublisher {

        private final List<WhatsappTranscriptExtractionRequestedEvent> events = new ArrayList<>();

        @Override
        public void publishRequested(WhatsappTranscriptExtractionRequestedEvent event) {
            events.add(event);
        }
    }
}
