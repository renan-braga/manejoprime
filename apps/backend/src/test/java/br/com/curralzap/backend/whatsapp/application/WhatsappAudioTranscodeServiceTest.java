package br.com.curralzap.backend.whatsapp.application;

import br.com.curralzap.backend.whatsapp.domain.WhatsappAudioMedia;
import br.com.curralzap.backend.whatsapp.domain.WhatsappInboundMessage;
import br.com.curralzap.backend.whatsapp.domain.WhatsappProcessingStatus;
import br.com.curralzap.backend.whatsapp.domain.WhatsappProcessingStep;
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

class WhatsappAudioTranscodeServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-04-04T11:30:00Z"), ZoneOffset.UTC);

    @Test
    void transcodesDownloadedAudioPersistsMetadataAdvancesStatusAndPublishesTranscriptionRequest() {
        InMemoryWhatsappInboundMessageRepository inboundMessages = new InMemoryWhatsappInboundMessageRepository();
        InMemoryWhatsappAudioMediaRepository audioMediaRepository = new InMemoryWhatsappAudioMediaRepository();
        InMemoryWhatsappAudioStorage storage = new InMemoryWhatsappAudioStorage();
        CapturingWhatsappAudioTranscoder transcoder = new CapturingWhatsappAudioTranscoder();
        CapturingWhatsappAudioTranscriptionRequestedPublisher publisher =
                new CapturingWhatsappAudioTranscriptionRequestedPublisher();
        WhatsappAudioTranscodeService service = new WhatsappAudioTranscodeService(
                inboundMessages,
                audioMediaRepository,
                storage,
                transcoder,
                publisher,
                FIXED_CLOCK
        );
        WhatsappInboundMessage inboundMessage = inboundMessages.save(WhatsappInboundMessage.receive(
                "wamid.transcode-1",
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
                WhatsappProcessingStatus.MEDIA_DOWNLOADED,
                "Audio downloaded to whatsapp/audio/wamid.transcode-1.ogg.",
                OffsetDateTime.parse("2026-04-04T11:05:00Z")
        );
        audioMediaRepository.save(WhatsappAudioMedia.downloaded(
                inboundMessage.id(),
                "https://media.example/audio-transcode-1.ogg",
                "audio/ogg",
                "whatsapp/audio/wamid.transcode-1.ogg",
                OffsetDateTime.parse("2026-04-04T11:05:00Z")
        ));
        storage.files.put("whatsapp/audio/wamid.transcode-1.ogg", "downloaded-audio".getBytes(StandardCharsets.UTF_8));

        service.handleRequestedTranscode(new WhatsappAudioTranscodeRequestedEvent(
                1,
                "whatsapp.audio.transcode.requested.v1",
                inboundMessage.id().toString(),
                "req-wa-006-transcode-ok",
                OffsetDateTime.parse("2026-04-04T11:05:00Z"),
                inboundMessage.id(),
                inboundMessage.providerMessageId(),
                "whatsapp/audio/wamid.transcode-1.ogg",
                "audio/ogg"
        ));

        assertThat(transcoder.requests).singleElement().satisfies(request -> {
            assertThat(request.providerMessageId()).isEqualTo("wamid.transcode-1");
            assertThat(request.sourceMimeType()).isEqualTo("audio/ogg");
        });
        assertThat(storage.storeRequests).singleElement().satisfies(request -> {
            assertThat(request.storageKey()).isEqualTo("wamid.transcode-1-transcoded");
            assertThat(request.mimeType()).isEqualTo("audio/mpeg");
        });
        assertThat(audioMediaRepository.findByInboundMessageId(inboundMessage.id()))
                .get()
                .satisfies(media -> {
                    assertThat(media.transcodedMimeType()).isEqualTo("audio/mpeg");
                    assertThat(media.transcodedStoragePath()).isEqualTo("whatsapp/audio/wamid.transcode-1-transcoded.mp3");
                    assertThat(media.transcodedAt()).isEqualTo(OffsetDateTime.now(FIXED_CLOCK));
                });
        assertThat(publisher.events).singleElement().satisfies(event -> {
            assertThat(event.providerMessageId()).isEqualTo("wamid.transcode-1");
            assertThat(event.sourceStoragePath()).isEqualTo("whatsapp/audio/wamid.transcode-1-transcoded.mp3");
            assertThat(event.sourceMimeType()).isEqualTo("audio/mpeg");
        });
        assertThat(inboundMessages.findByProviderMessageId("wamid.transcode-1"))
                .get()
                .extracting(WhatsappInboundMessage::status)
                .isEqualTo(WhatsappProcessingStatus.TRANSCODED);
        assertThat(inboundMessages.findProcessingSteps(inboundMessage.id()))
                .extracting(WhatsappProcessingStep::status, WhatsappProcessingStep::details)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(WhatsappProcessingStatus.RECEIVED, null),
                        org.assertj.core.groups.Tuple.tuple(
                                WhatsappProcessingStatus.MEDIA_DOWNLOADED,
                                "Audio downloaded to whatsapp/audio/wamid.transcode-1.ogg."
                        ),
                        org.assertj.core.groups.Tuple.tuple(
                                WhatsappProcessingStatus.TRANSCODED,
                                "Audio transcoded to whatsapp/audio/wamid.transcode-1-transcoded.mp3."
                        )
                );
    }

    @Test
    void republishesTranscriptionRequestWhenAudioIsAlreadyTranscodedOnRetry() {
        InMemoryWhatsappInboundMessageRepository inboundMessages = new InMemoryWhatsappInboundMessageRepository();
        InMemoryWhatsappAudioMediaRepository audioMediaRepository = new InMemoryWhatsappAudioMediaRepository();
        InMemoryWhatsappAudioStorage storage = new InMemoryWhatsappAudioStorage();
        CapturingWhatsappAudioTranscoder transcoder = new CapturingWhatsappAudioTranscoder();
        CapturingWhatsappAudioTranscriptionRequestedPublisher publisher =
                new CapturingWhatsappAudioTranscriptionRequestedPublisher();
        WhatsappAudioTranscodeService service = new WhatsappAudioTranscodeService(
                inboundMessages,
                audioMediaRepository,
                storage,
                transcoder,
                publisher,
                FIXED_CLOCK
        );
        WhatsappInboundMessage inboundMessage = inboundMessages.save(WhatsappInboundMessage.receive(
                "wamid.transcode-2",
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
                WhatsappProcessingStatus.TRANSCODED,
                "Audio transcoded to whatsapp/audio/wamid.transcode-2-transcoded.mp3.",
                OffsetDateTime.parse("2026-04-04T11:06:00Z")
        );
        audioMediaRepository.save(WhatsappAudioMedia.downloaded(
                inboundMessage.id(),
                "https://media.example/audio-transcode-2.ogg",
                "audio/ogg",
                "whatsapp/audio/wamid.transcode-2.ogg",
                OffsetDateTime.parse("2026-04-04T11:05:00Z")
        ).markTranscoded(
                "audio/mpeg",
                "whatsapp/audio/wamid.transcode-2-transcoded.mp3",
                OffsetDateTime.parse("2026-04-04T11:06:00Z")
        ));

        service.handleRequestedTranscode(new WhatsappAudioTranscodeRequestedEvent(
                1,
                "whatsapp.audio.transcode.requested.v1",
                inboundMessage.id().toString(),
                "req-wa-006-transcode-retry",
                OffsetDateTime.parse("2026-04-04T11:06:00Z"),
                inboundMessage.id(),
                inboundMessage.providerMessageId(),
                "whatsapp/audio/wamid.transcode-2.ogg",
                "audio/ogg"
        ));

        assertThat(transcoder.requests).isEmpty();
        assertThat(storage.storeRequests).isEmpty();
        assertThat(publisher.events).singleElement().satisfies(event ->
                assertThat(event.sourceStoragePath()).isEqualTo("whatsapp/audio/wamid.transcode-2-transcoded.mp3"));
    }

    @Test
    void recordsFailedStateWhenTranscoderRejectsInputAndDoesNotPublish() {
        InMemoryWhatsappInboundMessageRepository inboundMessages = new InMemoryWhatsappInboundMessageRepository();
        InMemoryWhatsappAudioMediaRepository audioMediaRepository = new InMemoryWhatsappAudioMediaRepository();
        InMemoryWhatsappAudioStorage storage = new InMemoryWhatsappAudioStorage();
        CapturingWhatsappAudioTranscoder transcoder = new CapturingWhatsappAudioTranscoder();
        CapturingWhatsappAudioTranscriptionRequestedPublisher publisher =
                new CapturingWhatsappAudioTranscriptionRequestedPublisher();
        transcoder.fail = true;
        WhatsappAudioTranscodeService service = new WhatsappAudioTranscodeService(
                inboundMessages,
                audioMediaRepository,
                storage,
                transcoder,
                publisher,
                FIXED_CLOCK
        );
        WhatsappInboundMessage inboundMessage = inboundMessages.save(WhatsappInboundMessage.receive(
                "wamid.transcode-fail",
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
                WhatsappProcessingStatus.MEDIA_DOWNLOADED,
                "Audio downloaded to whatsapp/audio/wamid.transcode-fail.ogg.",
                OffsetDateTime.parse("2026-04-04T11:05:00Z")
        );
        audioMediaRepository.save(WhatsappAudioMedia.downloaded(
                inboundMessage.id(),
                "https://media.example/audio-transcode-fail.ogg",
                "audio/ogg",
                "whatsapp/audio/wamid.transcode-fail.ogg",
                OffsetDateTime.parse("2026-04-04T11:05:00Z")
        ));
        storage.files.put("whatsapp/audio/wamid.transcode-fail.ogg", "downloaded-audio".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.handleRequestedTranscode(new WhatsappAudioTranscodeRequestedEvent(
                1,
                "whatsapp.audio.transcode.requested.v1",
                inboundMessage.id().toString(),
                "req-wa-006-transcode-fail",
                OffsetDateTime.parse("2026-04-04T11:05:00Z"),
                inboundMessage.id(),
                inboundMessage.providerMessageId(),
                "whatsapp/audio/wamid.transcode-fail.ogg",
                "audio/ogg"
        )))
                .isInstanceOf(WhatsappWorkerRetryableException.class)
                .hasMessage("Failed to process WhatsApp audio transcode worker event.");

        assertThat(inboundMessages.findByProviderMessageId("wamid.transcode-fail"))
                .get()
                .extracting(WhatsappInboundMessage::status)
                .isEqualTo(WhatsappProcessingStatus.FAILED);
        assertThat(inboundMessages.findProcessingSteps(inboundMessage.id()))
                .last()
                .satisfies(step -> {
                    assertThat(step.status()).isEqualTo(WhatsappProcessingStatus.FAILED);
                    assertThat(step.details()).isEqualTo("Audio transcode failed: Unsupported input codec.");
                });
        assertThat(publisher.events).isEmpty();
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

    private static final class InMemoryWhatsappAudioStorage implements WhatsappAudioStorage {

        private final Map<String, byte[]> files = new LinkedHashMap<>();
        private final List<StoreWhatsappAudioRequest> storeRequests = new ArrayList<>();

        @Override
        public String store(StoreWhatsappAudioRequest request) {
            storeRequests.add(request);
            String storagePath = "whatsapp/audio/" + request.storageKey() + ".mp3";
            files.put(storagePath, request.content());
            return storagePath;
        }

        @Override
        public byte[] load(String storagePath) {
            return files.get(storagePath);
        }
    }

    private static final class CapturingWhatsappAudioTranscoder implements WhatsappAudioTranscoder {

        private final List<WhatsappAudioTranscodeRequest> requests = new ArrayList<>();
        private boolean fail;

        @Override
        public TranscodedWhatsappAudio transcode(WhatsappAudioTranscodeRequest request) {
            requests.add(request);
            if (fail) {
                throw new IllegalStateException("Unsupported input codec.");
            }
            return new TranscodedWhatsappAudio(
                    "audio/mpeg",
                    "transcoded-audio".getBytes(StandardCharsets.UTF_8)
            );
        }
    }

    private static final class CapturingWhatsappAudioTranscriptionRequestedPublisher
            implements WhatsappAudioTranscriptionRequestedPublisher {

        private final List<WhatsappAudioTranscriptionRequestedEvent> events = new ArrayList<>();

        @Override
        public void publishRequested(WhatsappAudioTranscriptionRequestedEvent event) {
            events.add(event);
        }
    }
}
