package br.com.curralzap.backend.whatsapp.application;

import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.auth.application.AuthTokenType;
import br.com.curralzap.backend.auth.application.LoginUserPolicy;
import br.com.curralzap.backend.support.InMemoryUserAccountRepository;
import br.com.curralzap.backend.support.TestUserAccounts;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserRole;
import br.com.curralzap.backend.whatsapp.domain.WhatsappAudioMedia;
import br.com.curralzap.backend.whatsapp.domain.WhatsappExtractedPayload;
import br.com.curralzap.backend.whatsapp.domain.WhatsappInboundMessage;
import br.com.curralzap.backend.whatsapp.domain.WhatsappOutboundMessage;
import br.com.curralzap.backend.whatsapp.domain.WhatsappProcessingStatus;
import br.com.curralzap.backend.whatsapp.domain.WhatsappProcessingStep;
import br.com.curralzap.backend.whatsapp.domain.WhatsappTranscript;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WhatsappSupportServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-04-29T18:30:00Z"), ZoneOffset.UTC);

    @Test
    void detailIncludesRetryPlanForAudioTranscodeFailures() {
        Fixture fixture = new Fixture();
        UserAccount admin = fixture.admin();
        WhatsappInboundMessage inboundMessage = fixture.inboundMessage(
                "wamid.transcode.failed",
                WhatsappProcessingStatus.FAILED,
                "Audio transcode failed: Unsupported input codec."
        );
        fixture.audioMediaRepository.save(WhatsappAudioMedia.downloaded(
                inboundMessage.id(),
                "https://evolution.test/media/audio.ogg",
                "audio/ogg",
                "whatsapp/inbound/audio.ogg",
                OffsetDateTime.now(FIXED_CLOCK).minusMinutes(4)
        ));
        fixture.transcriptRepository.save(WhatsappTranscript.transcribed(
                inboundMessage.id(),
                "openai",
                "gpt-5.4-mini",
                "Peso 520 quilos",
                OffsetDateTime.now(FIXED_CLOCK).minusMinutes(3)
        ));
        fixture.extractedPayloadRepository.save(WhatsappExtractedPayload.extracted(
                inboundMessage.id(),
                "openai",
                "gpt-5.4-mini",
                "register_weighing",
                "{\"intent\":\"register_weighing\"}",
                OffsetDateTime.now(FIXED_CLOCK).minusMinutes(2)
        ));

        WhatsappSupportMessageDetailView detail = fixture.service.detail(fixture.claims(admin), inboundMessage.id());

        assertThat(detail.retry().supported()).isTrue();
        assertThat(detail.retry().action()).isEqualTo("audio_transcode_requested");
        assertThat(detail.processingSteps()).isNotEmpty();
        assertThat(detail.transcript()).isNotNull();
        assertThat(detail.extractedPayload()).isNotNull();
    }

    @Test
    void retryRepublishesInitialReceivedEventWhenTheFirstPublishFailed() {
        Fixture fixture = new Fixture();
        UserAccount admin = fixture.admin();
        WhatsappInboundMessage inboundMessage = fixture.inboundMessage(
                "wamid.initial.publish.failed",
                WhatsappProcessingStatus.FAILED,
                "Received event publish failed: Kafka unavailable."
        );

        WhatsappSupportRetryView result = fixture.service.retry(fixture.claims(admin), inboundMessage.id());

        assertThat(result.supported()).isTrue();
        assertThat(result.action()).isEqualTo("message_received");
        assertThat(fixture.receivedPublisher.events).singleElement()
                .satisfies(event -> {
                    assertThat(event.inboundMessageId()).isEqualTo(inboundMessage.id());
                    assertThat(event.providerMessageId()).isEqualTo("wamid.initial.publish.failed");
                });
    }

    @Test
    void retryRepublishesOutboundDeliveryWhenTheLatestOutboundFailed() {
        Fixture fixture = new Fixture();
        UserAccount admin = fixture.admin();
        WhatsappInboundMessage inboundMessage = fixture.inboundMessage(
                "wamid.outbound.failed",
                WhatsappProcessingStatus.REJECTED,
                "Validation rejected missing animal 347."
        );
        fixture.outboundMessageRepository.save(WhatsappOutboundMessage.queue(
                inboundMessage.id(),
                inboundMessage.farmId(),
                inboundMessage.userId(),
                inboundMessage.correlationId(),
                inboundMessage.phoneNumber(),
                "text",
                "{\"text\":\"Nao encontrei o animal informado.\"}",
                OffsetDateTime.now(FIXED_CLOCK).minusMinutes(1)
        ).markFailed("Evolution sendText timed out.", OffsetDateTime.now(FIXED_CLOCK)));

        WhatsappSupportRetryView result = fixture.service.retry(fixture.claims(admin), inboundMessage.id());

        assertThat(result.supported()).isTrue();
        assertThat(result.action()).isEqualTo("outbound_delivery_requested");
        assertThat(fixture.outboundDeliveryPublisher.events).singleElement()
                .extracting(WhatsappOutboundDeliveryRequestedEvent::status)
                .isEqualTo(WhatsappProcessingStatus.REJECTED);
    }

    @Test
    void retryRejectsUnsupportedBusinessRejections() {
        Fixture fixture = new Fixture();
        UserAccount admin = fixture.admin();
        WhatsappInboundMessage inboundMessage = fixture.inboundMessage(
                "wamid.unsupported.retry",
                WhatsappProcessingStatus.REJECTED,
                "Validation rejected unsupported intent: unknown."
        );

        assertThatThrownBy(() -> fixture.service.retry(fixture.claims(admin), inboundMessage.id()))
                .isInstanceOf(WhatsappSupportRetryNotAllowedException.class)
                .hasMessage("Only failed or outbound-delivery-pending messages can be retried.");
    }

    private static final class Fixture {

        private final InMemoryUserAccountRepository userAccountRepository = new InMemoryUserAccountRepository();
        private final LoginUserPolicy loginUserPolicy = new LoginUserPolicy();
        private final CapturingWhatsappSupportReadViewRepository supportReadViewRepository =
                new CapturingWhatsappSupportReadViewRepository();
        private final InMemoryWhatsappInboundMessageRepository inboundMessageRepository =
                new InMemoryWhatsappInboundMessageRepository();
        private final InMemoryWhatsappOutboundMessageRepository outboundMessageRepository =
                new InMemoryWhatsappOutboundMessageRepository();
        private final InMemoryWhatsappAudioMediaRepository audioMediaRepository =
                new InMemoryWhatsappAudioMediaRepository();
        private final InMemoryWhatsappTranscriptRepository transcriptRepository =
                new InMemoryWhatsappTranscriptRepository();
        private final InMemoryWhatsappExtractedPayloadRepository extractedPayloadRepository =
                new InMemoryWhatsappExtractedPayloadRepository();
        private final CapturingWhatsappReceivedMessagePublisher receivedPublisher =
                new CapturingWhatsappReceivedMessagePublisher();
        private final CapturingWhatsappAudioTranscodeRequestedPublisher audioTranscodePublisher =
                new CapturingWhatsappAudioTranscodeRequestedPublisher();
        private final CapturingWhatsappAudioTranscriptionRequestedPublisher audioTranscriptionPublisher =
                new CapturingWhatsappAudioTranscriptionRequestedPublisher();
        private final CapturingWhatsappTranscriptExtractionRequestedPublisher transcriptExtractionPublisher =
                new CapturingWhatsappTranscriptExtractionRequestedPublisher();
        private final CapturingWhatsappOutboundDeliveryRequestedPublisher outboundDeliveryPublisher =
                new CapturingWhatsappOutboundDeliveryRequestedPublisher();
        private final WhatsappSupportService service = new WhatsappSupportService(
                userAccountRepository,
                loginUserPolicy,
                supportReadViewRepository,
                inboundMessageRepository,
                outboundMessageRepository,
                audioMediaRepository,
                transcriptRepository,
                extractedPayloadRepository,
                receivedPublisher,
                audioTranscodePublisher,
                audioTranscriptionPublisher,
                transcriptExtractionPublisher,
                outboundDeliveryPublisher,
                FIXED_CLOCK
        );

        private UserAccount admin() {
            UserAccount userAccount = TestUserAccounts.userAccount(
                    UUID.fromString("00000000-0000-0000-0000-000000000111"),
                    "5511999999001",
                    "Admin Support",
                    UserRole.ADMIN,
                    true,
                    OffsetDateTime.now(FIXED_CLOCK).minusDays(1)
            );
            return userAccountRepository.save(userAccount);
        }

        private AuthTokenClaims claims(UserAccount userAccount) {
            OffsetDateTime issuedAt = OffsetDateTime.now(FIXED_CLOCK).minusMinutes(20);
            return new AuthTokenClaims(
                    userAccount.id(),
                    userAccount.farmId(),
                    userAccount.role(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    AuthTokenType.ACCESS,
                    issuedAt,
                    issuedAt.plusHours(1)
            );
        }

        private WhatsappInboundMessage inboundMessage(
                String providerMessageId,
                WhatsappProcessingStatus status,
                String details
        ) {
            UserAccount admin = admin();
            WhatsappInboundMessage received = WhatsappInboundMessage.receive(
                    providerMessageId,
                    "messages.upsert",
                    admin.phone(),
                    admin.farmId(),
                    admin.id(),
                    "req-support-" + providerMessageId,
                    "audioMessage",
                    "{\"event\":\"messages.upsert\"}",
                    OffsetDateTime.now(FIXED_CLOCK).minusMinutes(5)
            );
            WhatsappInboundMessage saved = inboundMessageRepository.save(received);
            return inboundMessageRepository.recordProcessingStep(
                    saved.id(),
                    status,
                    details,
                    OffsetDateTime.now(FIXED_CLOCK).minusMinutes(1)
            );
        }
    }

    private static final class CapturingWhatsappSupportReadViewRepository implements WhatsappSupportReadViewRepository {

        @Override
        public WhatsappSupportMessageListView listFor(UUID farmId, UUID userId, WhatsappSupportMessageQuery query) {
            return new WhatsappSupportMessageListView(List.of(), 0, query.limit(), query.offset());
        }
    }

    private static final class CapturingWhatsappReceivedMessagePublisher implements WhatsappReceivedMessagePublisher {

        private final List<WhatsappMessageReceivedEvent> events = new ArrayList<>();

        @Override
        public void publishReceived(WhatsappMessageReceivedEvent event) {
            events.add(event);
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

    private static final class CapturingWhatsappAudioTranscriptionRequestedPublisher
            implements WhatsappAudioTranscriptionRequestedPublisher {

        private final List<WhatsappAudioTranscriptionRequestedEvent> events = new ArrayList<>();

        @Override
        public void publishRequested(WhatsappAudioTranscriptionRequestedEvent event) {
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

    private static final class CapturingWhatsappOutboundDeliveryRequestedPublisher
            implements WhatsappOutboundDeliveryRequestedPublisher {

        private final List<WhatsappOutboundDeliveryRequestedEvent> events = new ArrayList<>();

        @Override
        public void publishRequested(WhatsappOutboundDeliveryRequestedEvent event) {
            events.add(event);
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
            stepsByMessageId.computeIfAbsent(persisted.id(), ignored -> new ArrayList<>());
            return persisted;
        }

        @Override
        public Optional<WhatsappInboundMessage> findById(UUID inboundMessageId) {
            return Optional.ofNullable(messagesById.get(inboundMessageId));
        }

        @Override
        public Optional<WhatsappInboundMessage> findByProviderMessageId(String providerMessageId) {
            return messagesById.values().stream()
                    .filter(message -> providerMessageId.equals(message.providerMessageId()))
                    .findFirst();
        }

        @Override
        public List<WhatsappInboundMessage> findRecentByFarmId(UUID farmId, int limit) {
            return messagesById.values().stream()
                    .filter(message -> farmId.equals(message.farmId()))
                    .sorted(Comparator.comparing(WhatsappInboundMessage::createdAt).reversed())
                    .limit(limit)
                    .toList();
        }

        @Override
        public List<WhatsappProcessingStep> findProcessingSteps(UUID inboundMessageId) {
            return List.copyOf(stepsByMessageId.getOrDefault(inboundMessageId, List.of()));
        }

        @Override
        public WhatsappInboundMessage recordProcessingStep(
                UUID inboundMessageId,
                WhatsappProcessingStatus status,
                String details,
                OffsetDateTime occurredAt
        ) {
            WhatsappInboundMessage current = messagesById.get(inboundMessageId);
            WhatsappInboundMessage updated = current.withProcessingStatus(status, details, occurredAt);
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

    private static final class InMemoryWhatsappOutboundMessageRepository implements WhatsappOutboundMessageRepository {

        private final Map<UUID, WhatsappOutboundMessage> messagesById = new HashMap<>();

        @Override
        public WhatsappOutboundMessage save(WhatsappOutboundMessage outboundMessage) {
            UUID id = outboundMessage.id() == null ? UUID.randomUUID() : outboundMessage.id();
            WhatsappOutboundMessage persisted = new WhatsappOutboundMessage(
                    id,
                    outboundMessage.inboundMessageId(),
                    outboundMessage.farmId(),
                    outboundMessage.userId(),
                    outboundMessage.correlationId(),
                    outboundMessage.phoneNumber(),
                    outboundMessage.messageType(),
                    outboundMessage.payload(),
                    outboundMessage.providerMessageId(),
                    outboundMessage.status(),
                    outboundMessage.failureReason(),
                    outboundMessage.queuedAt(),
                    outboundMessage.sentAt(),
                    outboundMessage.createdAt(),
                    outboundMessage.updatedAt()
            );
            messagesById.put(id, persisted);
            return persisted;
        }

        @Override
        public Optional<WhatsappOutboundMessage> findById(UUID outboundMessageId) {
            return Optional.ofNullable(messagesById.get(outboundMessageId));
        }

        @Override
        public Optional<WhatsappOutboundMessage> findByProviderMessageId(String providerMessageId) {
            return messagesById.values().stream()
                    .filter(message -> providerMessageId.equals(message.providerMessageId()))
                    .findFirst();
        }

        @Override
        public List<WhatsappOutboundMessage> findByInboundMessageIdOrderByCreatedAtAsc(UUID inboundMessageId) {
            return messagesById.values().stream()
                    .filter(message -> inboundMessageId.equals(message.inboundMessageId()))
                    .sorted(Comparator.comparing(WhatsappOutboundMessage::createdAt))
                    .toList();
        }
    }

    private static final class InMemoryWhatsappAudioMediaRepository implements WhatsappAudioMediaRepository {

        private final Map<UUID, WhatsappAudioMedia> mediaByInboundMessageId = new HashMap<>();

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

        private final Map<UUID, WhatsappTranscript> transcriptsByInboundMessageId = new HashMap<>();

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

    private static final class InMemoryWhatsappExtractedPayloadRepository implements WhatsappExtractedPayloadRepository {

        private final Map<UUID, WhatsappExtractedPayload> payloadsByInboundMessageId = new HashMap<>();

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
}
