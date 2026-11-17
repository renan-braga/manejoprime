package br.com.curralzap.backend.whatsapp.application;

import br.com.curralzap.backend.whatsapp.domain.WhatsappAudioMedia;
import br.com.curralzap.backend.whatsapp.domain.WhatsappInboundMessage;
import br.com.curralzap.backend.whatsapp.domain.WhatsappProcessingStatus;
import br.com.curralzap.backend.whatsapp.domain.WhatsappProcessingStep;
import br.com.curralzap.backend.whatsapp.domain.WhatsappTranscript;
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

class WhatsappAudioTranscriptionServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-04-04T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void transcribesTranscodedAudioPersistsTranscriptAndAdvancesStatus() {
        InMemoryWhatsappInboundMessageRepository inboundMessages = new InMemoryWhatsappInboundMessageRepository();
        InMemoryWhatsappAudioMediaRepository audioMediaRepository = new InMemoryWhatsappAudioMediaRepository();
        InMemoryWhatsappTranscriptRepository transcriptRepository = new InMemoryWhatsappTranscriptRepository();
        InMemoryWhatsappAudioStorage storage = new InMemoryWhatsappAudioStorage();
        CapturingWhatsappAudioTranscriber transcriber = new CapturingWhatsappAudioTranscriber();
        CapturingWhatsappTranscriptExtractionRequestedPublisher extractionRequestedPublisher =
                new CapturingWhatsappTranscriptExtractionRequestedPublisher();
        WhatsappAudioTranscriptionService service = new WhatsappAudioTranscriptionService(
                inboundMessages,
                audioMediaRepository,
                transcriptRepository,
                storage,
                transcriber,
                extractionRequestedPublisher,
                FIXED_CLOCK
        );
        WhatsappInboundMessage inboundMessage = inboundMessages.save(WhatsappInboundMessage.receive(
                "wamid.transcript-1",
                "messages.upsert",
                "5511999990001",
                null,
                null,
                "audioMessage",
                "{\"event\":\"messages.upsert\"}",
                OffsetDateTime.parse("2026-04-04T11:00:00Z")
        ));
        inboundMessages.recordProcessingStep(
                inboundMessage.id(),
                WhatsappProcessingStatus.TRANSCODED,
                "Audio transcoded to whatsapp/audio/wamid.transcript-1.mp3.",
                OffsetDateTime.parse("2026-04-04T11:10:00Z")
        );
        audioMediaRepository.save(WhatsappAudioMedia.downloaded(
                inboundMessage.id(),
                "https://media.example/audio-transcript-1.ogg",
                "audio/ogg",
                "whatsapp/audio/wamid.transcript-1.ogg",
                OffsetDateTime.parse("2026-04-04T11:05:00Z")
        ).markTranscoded(
                "audio/mpeg",
                "whatsapp/audio/wamid.transcript-1.mp3",
                OffsetDateTime.parse("2026-04-04T11:10:00Z")
        ));
        storage.files.put("whatsapp/audio/wamid.transcript-1.mp3", "transcoded-audio".getBytes(StandardCharsets.UTF_8));

        service.handleRequestedTranscription(new WhatsappAudioTranscriptionRequestedEvent(
                1,
                "whatsapp.audio.transcription.requested.v1",
                inboundMessage.id().toString(),
                "req-wa-006-transcription-ok",
                OffsetDateTime.parse("2026-04-04T11:10:00Z"),
                inboundMessage.id(),
                inboundMessage.providerMessageId(),
                "whatsapp/audio/wamid.transcript-1.mp3",
                "audio/mpeg"
        ));

        assertThat(transcriber.requests).singleElement().satisfies(request -> {
            assertThat(request.providerMessageId()).isEqualTo("wamid.transcript-1");
            assertThat(request.mimeType()).isEqualTo("audio/mpeg");
        });
        assertThat(transcriptRepository.findByInboundMessageId(inboundMessage.id()))
                .get()
                .satisfies(transcript -> {
                    assertThat(transcript.provider()).isEqualTo("openai");
                    assertThat(transcript.providerModel()).isEqualTo("gpt-4o-mini-transcribe");
                    assertThat(transcript.transcript()).isEqualTo("Registrar peso de 18 arrobas.");
                    assertThat(transcript.transcribedAt()).isEqualTo(OffsetDateTime.now(FIXED_CLOCK));
                });
        assertThat(inboundMessages.findByProviderMessageId("wamid.transcript-1"))
                .get()
                .extracting(WhatsappInboundMessage::status)
                .isEqualTo(WhatsappProcessingStatus.TRANSCRIBED);
        assertThat(inboundMessages.findProcessingSteps(inboundMessage.id()))
                .extracting(WhatsappProcessingStep::status, WhatsappProcessingStep::details)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(WhatsappProcessingStatus.RECEIVED, null),
                        org.assertj.core.groups.Tuple.tuple(
                                WhatsappProcessingStatus.TRANSCODED,
                                "Audio transcoded to whatsapp/audio/wamid.transcript-1.mp3."
                        ),
                        org.assertj.core.groups.Tuple.tuple(
                                WhatsappProcessingStatus.TRANSCRIBED,
                                "Audio transcribed: Registrar peso de 18 arrobas."
                        )
                );
        assertThat(extractionRequestedPublisher.events).singleElement().satisfies(event -> {
            assertThat(event.schemaVersion()).isEqualTo(1);
            assertThat(event.eventType()).isEqualTo("whatsapp.transcript.extraction.requested.v1");
            assertThat(event.eventId()).isEqualTo(inboundMessage.id().toString());
            assertThat(event.correlationId()).isEqualTo("req-wa-006-transcription-ok");
            assertThat(event.occurredAt()).isEqualTo(OffsetDateTime.now(FIXED_CLOCK));
            assertThat(event.inboundMessageId()).isEqualTo(inboundMessage.id());
            assertThat(event.providerMessageId()).isEqualTo("wamid.transcript-1");
        });
    }

    @Test
    void republishesExtractionRequestForAlreadyTranscribedAudioWhenEventIsRetried() {
        InMemoryWhatsappInboundMessageRepository inboundMessages = new InMemoryWhatsappInboundMessageRepository();
        InMemoryWhatsappAudioMediaRepository audioMediaRepository = new InMemoryWhatsappAudioMediaRepository();
        InMemoryWhatsappTranscriptRepository transcriptRepository = new InMemoryWhatsappTranscriptRepository();
        InMemoryWhatsappAudioStorage storage = new InMemoryWhatsappAudioStorage();
        CapturingWhatsappAudioTranscriber transcriber = new CapturingWhatsappAudioTranscriber();
        CapturingWhatsappTranscriptExtractionRequestedPublisher extractionRequestedPublisher =
                new CapturingWhatsappTranscriptExtractionRequestedPublisher();
        WhatsappAudioTranscriptionService service = new WhatsappAudioTranscriptionService(
                inboundMessages,
                audioMediaRepository,
                transcriptRepository,
                storage,
                transcriber,
                extractionRequestedPublisher,
                FIXED_CLOCK
        );
        WhatsappInboundMessage inboundMessage = inboundMessages.save(WhatsappInboundMessage.receive(
                "wamid.transcript-2",
                "messages.upsert",
                "5511999990002",
                null,
                null,
                "audioMessage",
                "{\"event\":\"messages.upsert\"}",
                OffsetDateTime.parse("2026-04-04T11:01:00Z")
        ));
        inboundMessages.recordProcessingStep(
                inboundMessage.id(),
                WhatsappProcessingStatus.TRANSCRIBED,
                "Audio transcribed successfully.",
                OffsetDateTime.parse("2026-04-04T11:12:00Z")
        );
        transcriptRepository.save(WhatsappTranscript.transcribed(
                inboundMessage.id(),
                "openai",
                "gpt-4o-mini-transcribe",
                "Peso registrado.",
                OffsetDateTime.parse("2026-04-04T11:12:00Z")
        ));
        audioMediaRepository.save(WhatsappAudioMedia.downloaded(
                inboundMessage.id(),
                "https://media.example/audio-transcript-2.ogg",
                "audio/ogg",
                "whatsapp/audio/wamid.transcript-2.ogg",
                OffsetDateTime.parse("2026-04-04T11:05:00Z")
        ).markTranscoded(
                "audio/mpeg",
                "whatsapp/audio/wamid.transcript-2.mp3",
                OffsetDateTime.parse("2026-04-04T11:10:00Z")
        ));

        service.handleRequestedTranscription(new WhatsappAudioTranscriptionRequestedEvent(
                1,
                "whatsapp.audio.transcription.requested.v1",
                inboundMessage.id().toString(),
                "req-wa-006-transcription-retry",
                OffsetDateTime.parse("2026-04-04T11:12:00Z"),
                inboundMessage.id(),
                inboundMessage.providerMessageId(),
                "whatsapp/audio/wamid.transcript-2.mp3",
                "audio/mpeg"
        ));

        assertThat(transcriber.requests).isEmpty();
        assertThat(extractionRequestedPublisher.events).singleElement().satisfies(event -> {
            assertThat(event.correlationId()).isEqualTo("req-wa-006-transcription-retry");
            assertThat(event.inboundMessageId()).isEqualTo(inboundMessage.id());
            assertThat(event.providerMessageId()).isEqualTo("wamid.transcript-2");
        });
    }

    @Test
    void recordsFailedStateWhenTranscriberFailsOrReturnsMalformedTranscript() {
        InMemoryWhatsappInboundMessageRepository inboundMessages = new InMemoryWhatsappInboundMessageRepository();
        InMemoryWhatsappAudioMediaRepository audioMediaRepository = new InMemoryWhatsappAudioMediaRepository();
        InMemoryWhatsappTranscriptRepository transcriptRepository = new InMemoryWhatsappTranscriptRepository();
        InMemoryWhatsappAudioStorage storage = new InMemoryWhatsappAudioStorage();
        CapturingWhatsappAudioTranscriber transcriber = new CapturingWhatsappAudioTranscriber();
        transcriber.returnBlankTranscript = true;
        CapturingWhatsappTranscriptExtractionRequestedPublisher extractionRequestedPublisher =
                new CapturingWhatsappTranscriptExtractionRequestedPublisher();
        WhatsappAudioTranscriptionService service = new WhatsappAudioTranscriptionService(
                inboundMessages,
                audioMediaRepository,
                transcriptRepository,
                storage,
                transcriber,
                extractionRequestedPublisher,
                FIXED_CLOCK
        );
        WhatsappInboundMessage inboundMessage = inboundMessages.save(WhatsappInboundMessage.receive(
                "wamid.transcript-fail",
                "messages.upsert",
                "5511999990003",
                null,
                null,
                "audioMessage",
                "{\"event\":\"messages.upsert\"}",
                OffsetDateTime.parse("2026-04-04T11:02:00Z")
        ));
        inboundMessages.recordProcessingStep(
                inboundMessage.id(),
                WhatsappProcessingStatus.TRANSCODED,
                "Audio transcoded to whatsapp/audio/wamid.transcript-fail.mp3.",
                OffsetDateTime.parse("2026-04-04T11:10:00Z")
        );
        audioMediaRepository.save(WhatsappAudioMedia.downloaded(
                inboundMessage.id(),
                "https://media.example/audio-transcript-fail.ogg",
                "audio/ogg",
                "whatsapp/audio/wamid.transcript-fail.ogg",
                OffsetDateTime.parse("2026-04-04T11:05:00Z")
        ).markTranscoded(
                "audio/mpeg",
                "whatsapp/audio/wamid.transcript-fail.mp3",
                OffsetDateTime.parse("2026-04-04T11:10:00Z")
        ));
        storage.files.put("whatsapp/audio/wamid.transcript-fail.mp3", "transcoded-audio".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.handleRequestedTranscription(new WhatsappAudioTranscriptionRequestedEvent(
                1,
                "whatsapp.audio.transcription.requested.v1",
                inboundMessage.id().toString(),
                "req-wa-006-transcription-fail",
                OffsetDateTime.parse("2026-04-04T11:10:00Z"),
                inboundMessage.id(),
                inboundMessage.providerMessageId(),
                "whatsapp/audio/wamid.transcript-fail.mp3",
                "audio/mpeg"
        )))
                .isInstanceOf(WhatsappWorkerRetryableException.class)
                .hasMessage("Failed to process WhatsApp audio transcription worker event.");

        assertThat(transcriptRepository.findByInboundMessageId(inboundMessage.id())).isEmpty();
        assertThat(inboundMessages.findByProviderMessageId("wamid.transcript-fail"))
                .get()
                .extracting(WhatsappInboundMessage::status)
                .isEqualTo(WhatsappProcessingStatus.FAILED);
        assertThat(inboundMessages.findProcessingSteps(inboundMessage.id()))
                .last()
                .satisfies(step -> {
                    assertThat(step.status()).isEqualTo(WhatsappProcessingStatus.FAILED);
                    assertThat(step.details())
                            .isEqualTo("Audio transcription failed: Transcription provider returned an empty transcript.");
                });
        assertThat(extractionRequestedPublisher.events).isEmpty();
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

        private final Map<UUID, WhatsappAudioMedia> audioMediaByInboundMessageId = new LinkedHashMap<>();

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
            audioMediaByInboundMessageId.put(persisted.inboundMessageId(), persisted);
            return persisted;
        }

        @Override
        public Optional<WhatsappAudioMedia> findByInboundMessageId(UUID inboundMessageId) {
            return Optional.ofNullable(audioMediaByInboundMessageId.get(inboundMessageId));
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

    private static final class InMemoryWhatsappAudioStorage implements WhatsappAudioStorage {

        private final Map<String, byte[]> files = new LinkedHashMap<>();

        @Override
        public String store(StoreWhatsappAudioRequest request) {
            files.put("whatsapp/audio/" + request.storageKey() + ".mp3", request.content());
            return "whatsapp/audio/" + request.storageKey() + ".mp3";
        }

        @Override
        public byte[] load(String storagePath) {
            return files.get(storagePath);
        }
    }

    private static final class CapturingWhatsappAudioTranscriber implements WhatsappAudioTranscriber {

        private final List<WhatsappAudioTranscriptionRequest> requests = new ArrayList<>();
        private boolean returnBlankTranscript;

        @Override
        public WhatsappAudioTranscriptionResult transcribe(WhatsappAudioTranscriptionRequest request) {
            requests.add(request);
            if (returnBlankTranscript) {
                return new WhatsappAudioTranscriptionResult("openai", "gpt-4o-mini-transcribe", " ");
            }
            return new WhatsappAudioTranscriptionResult(
                    "openai",
                    "gpt-4o-mini-transcribe",
                    "Registrar peso de 18 arrobas."
            );
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
