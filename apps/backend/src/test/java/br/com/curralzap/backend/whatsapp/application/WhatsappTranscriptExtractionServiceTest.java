package br.com.curralzap.backend.whatsapp.application;

import br.com.curralzap.backend.whatsapp.domain.WhatsappExtractedPayload;
import br.com.curralzap.backend.whatsapp.domain.WhatsappInboundMessage;
import br.com.curralzap.backend.whatsapp.domain.WhatsappProcessingStatus;
import br.com.curralzap.backend.whatsapp.domain.WhatsappProcessingStep;
import br.com.curralzap.backend.whatsapp.domain.WhatsappTranscript;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

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

class WhatsappTranscriptExtractionServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-04-05T15:30:00Z"), ZoneOffset.UTC);

    @Test
    void extractsSupportedIntentPersistsStructuredPayloadAndAdvancesStatus() {
        InMemoryWhatsappInboundMessageRepository inboundMessages = new InMemoryWhatsappInboundMessageRepository();
        InMemoryWhatsappTranscriptRepository transcriptRepository = new InMemoryWhatsappTranscriptRepository();
        InMemoryWhatsappExtractedPayloadRepository extractedPayloadRepository =
                new InMemoryWhatsappExtractedPayloadRepository();
        CapturingWhatsappTranscriptExtractor extractor = new CapturingWhatsappTranscriptExtractor();
        WhatsappTranscriptExtractionService service = new WhatsappTranscriptExtractionService(
                inboundMessages,
                transcriptRepository,
                extractedPayloadRepository,
                extractor,
                FIXED_CLOCK
        );
        WhatsappInboundMessage inboundMessage = transcribedMessage(inboundMessages, transcriptRepository, "wamid.extract-ok");

        service.handleRequestedExtraction(new WhatsappTranscriptExtractionRequestedEvent(
                1,
                "whatsapp.transcript.extraction.requested.v1",
                inboundMessage.id().toString(),
                "corr-wa-007-ok",
                OffsetDateTime.parse("2026-04-05T15:20:00Z"),
                inboundMessage.id(),
                inboundMessage.providerMessageId()
        ));

        assertThat(extractor.requests).singleElement().satisfies(request -> {
            assertThat(request.providerMessageId()).isEqualTo("wamid.extract-ok");
            assertThat(request.transcript()).isEqualTo("Registrar peso do animal A-1 com 324,5 kg hoje.");
        });
        assertThat(extractedPayloadRepository.findByInboundMessageId(inboundMessage.id()))
                .get()
                .satisfies(payload -> {
                    assertThat(payload.provider()).isEqualTo("openai");
                    assertThat(payload.providerModel()).isEqualTo("gpt-5.4-mini");
                    assertThat(payload.intent()).isEqualTo("register_weighing");
                    assertThat(payload.structuredPayload())
                            .contains("\"intent\":\"register_weighing\"")
                            .contains("\"animalIdentifier\":\"A-1\"")
                            .contains("\"weightKg\":324.5")
                            .contains("\"confidence\":0.92");
                    assertThat(payload.extractedAt()).isEqualTo(OffsetDateTime.now(FIXED_CLOCK));
                });
        assertThat(inboundMessages.findByProviderMessageId("wamid.extract-ok"))
                .get()
                .extracting(WhatsappInboundMessage::status)
                .isEqualTo(WhatsappProcessingStatus.INTENT_EXTRACTED);
        assertThat(inboundMessages.findProcessingSteps(inboundMessage.id()))
                .last()
                .satisfies(step -> {
                    assertThat(step.status()).isEqualTo(WhatsappProcessingStatus.INTENT_EXTRACTED);
                    assertThat(step.details()).isEqualTo("Transcript extracted as register_weighing.");
                });
    }

    @Test
    void publishesPayloadValidationRequestAfterSuccessfulExtraction() {
        InMemoryWhatsappInboundMessageRepository inboundMessages = new InMemoryWhatsappInboundMessageRepository();
        InMemoryWhatsappTranscriptRepository transcriptRepository = new InMemoryWhatsappTranscriptRepository();
        InMemoryWhatsappExtractedPayloadRepository extractedPayloadRepository =
                new InMemoryWhatsappExtractedPayloadRepository();
        CapturingWhatsappTranscriptExtractor extractor = new CapturingWhatsappTranscriptExtractor();
        CapturingWhatsappPayloadValidationRequestedPublisher validationRequestedPublisher =
                new CapturingWhatsappPayloadValidationRequestedPublisher();
        WhatsappTranscriptExtractionService service = new WhatsappTranscriptExtractionService(
                inboundMessages,
                transcriptRepository,
                extractedPayloadRepository,
                extractor,
                validationRequestedPublisher,
                new ObjectMapper(),
                FIXED_CLOCK
        );
        WhatsappInboundMessage inboundMessage = transcribedMessage(inboundMessages, transcriptRepository, "wamid.extract-publish");

        service.handleRequestedExtraction(new WhatsappTranscriptExtractionRequestedEvent(
                1,
                "whatsapp.transcript.extraction.requested.v1",
                inboundMessage.id().toString(),
                "corr-wa-008-validation-publish",
                OffsetDateTime.parse("2026-04-05T15:20:00Z"),
                inboundMessage.id(),
                inboundMessage.providerMessageId()
        ));

        assertThat(validationRequestedPublisher.events).singleElement().satisfies(event -> {
            assertThat(event.schemaVersion()).isEqualTo(1);
            assertThat(event.eventType()).isEqualTo("whatsapp.payload.validation.requested.v1");
            assertThat(event.eventId()).isEqualTo(inboundMessage.id().toString());
            assertThat(event.correlationId()).isEqualTo("corr-wa-008-validation-publish");
            assertThat(event.occurredAt()).isEqualTo(OffsetDateTime.now(FIXED_CLOCK));
            assertThat(event.inboundMessageId()).isEqualTo(inboundMessage.id());
            assertThat(event.providerMessageId()).isEqualTo("wamid.extract-publish");
        });
    }

    @Test
    void storesUnknownIntentExplicitly() {
        InMemoryWhatsappInboundMessageRepository inboundMessages = new InMemoryWhatsappInboundMessageRepository();
        InMemoryWhatsappTranscriptRepository transcriptRepository = new InMemoryWhatsappTranscriptRepository();
        InMemoryWhatsappExtractedPayloadRepository extractedPayloadRepository =
                new InMemoryWhatsappExtractedPayloadRepository();
        CapturingWhatsappTranscriptExtractor extractor = new CapturingWhatsappTranscriptExtractor();
        extractor.result = WhatsappTranscriptExtractionResult.unknown(
                "openai",
                "gpt-5.4-mini",
                "The transcript does not describe a supported field workflow.",
                0.84
        );
        WhatsappTranscriptExtractionService service = new WhatsappTranscriptExtractionService(
                inboundMessages,
                transcriptRepository,
                extractedPayloadRepository,
                extractor,
                FIXED_CLOCK
        );
        WhatsappInboundMessage inboundMessage = transcribedMessage(inboundMessages, transcriptRepository, "wamid.extract-unknown");

        service.handleRequestedExtraction(new WhatsappTranscriptExtractionRequestedEvent(
                1,
                "whatsapp.transcript.extraction.requested.v1",
                inboundMessage.id().toString(),
                "corr-wa-007-unknown",
                OffsetDateTime.parse("2026-04-05T15:20:00Z"),
                inboundMessage.id(),
                inboundMessage.providerMessageId()
        ));

        assertThat(extractedPayloadRepository.findByInboundMessageId(inboundMessage.id()))
                .get()
                .satisfies(payload -> {
                    assertThat(payload.intent()).isEqualTo("unknown");
                    assertThat(payload.structuredPayload())
                            .contains("\"intent\":\"unknown\"")
                            .contains("\"reason\":\"The transcript does not describe a supported field workflow.\"");
                });
        assertThat(inboundMessages.findProcessingSteps(inboundMessage.id()))
                .last()
                .extracting(WhatsappProcessingStep::details)
                .isEqualTo("Transcript extraction produced unknown intent.");
    }

    @Test
    void storesLowConfidenceResultsExplicitly() {
        InMemoryWhatsappInboundMessageRepository inboundMessages = new InMemoryWhatsappInboundMessageRepository();
        InMemoryWhatsappTranscriptRepository transcriptRepository = new InMemoryWhatsappTranscriptRepository();
        InMemoryWhatsappExtractedPayloadRepository extractedPayloadRepository =
                new InMemoryWhatsappExtractedPayloadRepository();
        CapturingWhatsappTranscriptExtractor extractor = new CapturingWhatsappTranscriptExtractor();
        extractor.result = WhatsappTranscriptExtractionResult.supported(
                "openai",
                "gpt-5.4-mini",
                "register_occurrence",
                Map.of("animalIdentifier", "A-2", "description", "Animal looks tired."),
                0.51
        );
        WhatsappTranscriptExtractionService service = new WhatsappTranscriptExtractionService(
                inboundMessages,
                transcriptRepository,
                extractedPayloadRepository,
                extractor,
                FIXED_CLOCK
        );
        WhatsappInboundMessage inboundMessage = transcribedMessage(inboundMessages, transcriptRepository, "wamid.extract-low");

        service.handleRequestedExtraction(new WhatsappTranscriptExtractionRequestedEvent(
                1,
                "whatsapp.transcript.extraction.requested.v1",
                inboundMessage.id().toString(),
                "corr-wa-007-low",
                OffsetDateTime.parse("2026-04-05T15:20:00Z"),
                inboundMessage.id(),
                inboundMessage.providerMessageId()
        ));

        assertThat(extractedPayloadRepository.findByInboundMessageId(inboundMessage.id()))
                .get()
                .satisfies(payload -> {
                    assertThat(payload.intent()).isEqualTo("low_confidence");
                    assertThat(payload.structuredPayload())
                            .contains("\"intent\":\"low_confidence\"")
                            .contains("\"candidateIntent\":\"register_occurrence\"")
                            .contains("\"confidence\":0.51");
                });
        assertThat(inboundMessages.findProcessingSteps(inboundMessage.id()))
                .last()
                .extracting(WhatsappProcessingStep::details)
                .isEqualTo("Transcript extraction produced low confidence result for register_occurrence.");
    }

    @Test
    void ignoresAlreadyExtractedMessagesWhenEventIsRetried() {
        InMemoryWhatsappInboundMessageRepository inboundMessages = new InMemoryWhatsappInboundMessageRepository();
        InMemoryWhatsappTranscriptRepository transcriptRepository = new InMemoryWhatsappTranscriptRepository();
        InMemoryWhatsappExtractedPayloadRepository extractedPayloadRepository =
                new InMemoryWhatsappExtractedPayloadRepository();
        CapturingWhatsappTranscriptExtractor extractor = new CapturingWhatsappTranscriptExtractor();
        WhatsappTranscriptExtractionService service = new WhatsappTranscriptExtractionService(
                inboundMessages,
                transcriptRepository,
                extractedPayloadRepository,
                extractor,
                FIXED_CLOCK
        );
        WhatsappInboundMessage inboundMessage = transcribedMessage(inboundMessages, transcriptRepository, "wamid.extract-retry");
        extractedPayloadRepository.save(WhatsappExtractedPayload.extracted(
                inboundMessage.id(),
                "openai",
                "gpt-5.4-mini",
                "register_weighing",
                "{\"intent\":\"register_weighing\"}",
                OffsetDateTime.parse("2026-04-05T15:25:00Z")
        ));
        inboundMessages.recordProcessingStep(
                inboundMessage.id(),
                WhatsappProcessingStatus.INTENT_EXTRACTED,
                "Transcript extracted as register_weighing.",
                OffsetDateTime.parse("2026-04-05T15:25:00Z")
        );

        service.handleRequestedExtraction(new WhatsappTranscriptExtractionRequestedEvent(
                1,
                "whatsapp.transcript.extraction.requested.v1",
                inboundMessage.id().toString(),
                "corr-wa-007-retry",
                OffsetDateTime.parse("2026-04-05T15:26:00Z"),
                inboundMessage.id(),
                inboundMessage.providerMessageId()
        ));

        assertThat(extractor.requests).isEmpty();
    }

    @Test
    void recordsFailureWhenExtractionProviderFails() {
        InMemoryWhatsappInboundMessageRepository inboundMessages = new InMemoryWhatsappInboundMessageRepository();
        InMemoryWhatsappTranscriptRepository transcriptRepository = new InMemoryWhatsappTranscriptRepository();
        InMemoryWhatsappExtractedPayloadRepository extractedPayloadRepository =
                new InMemoryWhatsappExtractedPayloadRepository();
        CapturingWhatsappTranscriptExtractor extractor = new CapturingWhatsappTranscriptExtractor();
        extractor.fail = true;
        WhatsappTranscriptExtractionService service = new WhatsappTranscriptExtractionService(
                inboundMessages,
                transcriptRepository,
                extractedPayloadRepository,
                extractor,
                FIXED_CLOCK
        );
        WhatsappInboundMessage inboundMessage = transcribedMessage(inboundMessages, transcriptRepository, "wamid.extract-fail");

        assertThatThrownBy(() -> service.handleRequestedExtraction(new WhatsappTranscriptExtractionRequestedEvent(
                1,
                "whatsapp.transcript.extraction.requested.v1",
                inboundMessage.id().toString(),
                "corr-wa-007-fail",
                OffsetDateTime.parse("2026-04-05T15:20:00Z"),
                inboundMessage.id(),
                inboundMessage.providerMessageId()
        )))
                .isInstanceOf(WhatsappWorkerRetryableException.class)
                .hasMessage("Failed to process WhatsApp transcript extraction worker event.");

        assertThat(extractedPayloadRepository.findByInboundMessageId(inboundMessage.id())).isEmpty();
        assertThat(inboundMessages.findByProviderMessageId("wamid.extract-fail"))
                .get()
                .extracting(WhatsappInboundMessage::status)
                .isEqualTo(WhatsappProcessingStatus.FAILED);
        assertThat(inboundMessages.findProcessingSteps(inboundMessage.id()))
                .last()
                .satisfies(step -> {
                    assertThat(step.status()).isEqualTo(WhatsappProcessingStatus.FAILED);
                    assertThat(step.details()).isEqualTo("Transcript extraction failed: GPT extraction unavailable.");
                });
    }

    private WhatsappInboundMessage transcribedMessage(
            InMemoryWhatsappInboundMessageRepository inboundMessages,
            InMemoryWhatsappTranscriptRepository transcriptRepository,
            String providerMessageId
    ) {
        WhatsappInboundMessage inboundMessage = inboundMessages.save(WhatsappInboundMessage.receive(
                providerMessageId,
                "messages.upsert",
                "5511999990501",
                null,
                null,
                "audioMessage",
                "{\"event\":\"messages.upsert\"}",
                OffsetDateTime.parse("2026-04-05T15:00:00Z")
        ));
        inboundMessages.recordProcessingStep(
                inboundMessage.id(),
                WhatsappProcessingStatus.TRANSCRIBED,
                "Audio transcribed successfully.",
                OffsetDateTime.parse("2026-04-05T15:10:00Z")
        );
        transcriptRepository.save(WhatsappTranscript.transcribed(
                inboundMessage.id(),
                "openai",
                "gpt-4o-mini-transcribe",
                "Registrar peso do animal A-1 com 324,5 kg hoje.",
                OffsetDateTime.parse("2026-04-05T15:10:00Z")
        ));
        return inboundMessage;
    }

    private static final class CapturingWhatsappTranscriptExtractor implements WhatsappTranscriptExtractor {

        private final List<WhatsappTranscriptExtractionRequest> requests = new ArrayList<>();
        private WhatsappTranscriptExtractionResult result = WhatsappTranscriptExtractionResult.supported(
                "openai",
                "gpt-5.4-mini",
                "register_weighing",
                Map.of("animalIdentifier", "A-1", "weightKg", 324.5),
                0.92
        );
        private boolean fail;

        @Override
        public WhatsappTranscriptExtractionResult extract(WhatsappTranscriptExtractionRequest request) {
            requests.add(request);
            if (fail) {
                throw new IllegalStateException("GPT extraction unavailable.");
            }
            return result;
        }
    }

    private static final class CapturingWhatsappPayloadValidationRequestedPublisher
            implements WhatsappPayloadValidationRequestedPublisher {

        private final List<WhatsappPayloadValidationRequestedEvent> events = new ArrayList<>();

        @Override
        public void publishRequested(WhatsappPayloadValidationRequestedEvent event) {
            events.add(event);
        }
    }

    private static final class InMemoryWhatsappTranscriptRepository implements WhatsappTranscriptRepository {

        private final Map<UUID, WhatsappTranscript> transcriptsByInboundMessageId = new LinkedHashMap<>();

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
            transcriptsByInboundMessageId.put(persisted.inboundMessageId(), persisted);
            return persisted;
        }

        @Override
        public Optional<WhatsappTranscript> findByInboundMessageId(UUID inboundMessageId) {
            return Optional.ofNullable(transcriptsByInboundMessageId.get(inboundMessageId));
        }
    }

    private static final class InMemoryWhatsappExtractedPayloadRepository
            implements WhatsappExtractedPayloadRepository {

        private final Map<UUID, WhatsappExtractedPayload> payloadsByInboundMessageId = new LinkedHashMap<>();

        @Override
        public WhatsappExtractedPayload save(WhatsappExtractedPayload extractedPayload) {
            WhatsappExtractedPayload persisted = extractedPayload.id() == null
                    ? new WhatsappExtractedPayload(
                    UUID.randomUUID(),
                    extractedPayload.inboundMessageId(),
                    extractedPayload.provider(),
                    extractedPayload.providerModel(),
                    extractedPayload.intent(),
                    extractedPayload.structuredPayload(),
                    extractedPayload.extractedAt(),
                    extractedPayload.createdAt(),
                    extractedPayload.updatedAt()
            )
                    : extractedPayload;
            payloadsByInboundMessageId.put(persisted.inboundMessageId(), persisted);
            return persisted;
        }

        @Override
        public Optional<WhatsappExtractedPayload> findByInboundMessageId(UUID inboundMessageId) {
            return Optional.ofNullable(payloadsByInboundMessageId.get(inboundMessageId));
        }

        @Override
        public long countByFarmIdAndIntentAndReceivedAtBetween(
                UUID farmId,
                String intent,
                OffsetDateTime startInclusive,
                OffsetDateTime endExclusive
        ) {
            return 0;
        }
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
            stepsByMessageId.putIfAbsent(persisted.id(), new ArrayList<>(List.of(new WhatsappProcessingStep(
                    UUID.randomUUID(),
                    persisted.id(),
                    persisted.status(),
                    null,
                    persisted.receivedAt(),
                    persisted.createdAt(),
                    persisted.updatedAt()
            ))));
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
}
