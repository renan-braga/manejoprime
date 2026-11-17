package br.com.curralzap.backend.whatsapp.application;

import br.com.curralzap.backend.whatsapp.domain.WhatsappExtractedPayload;
import br.com.curralzap.backend.whatsapp.domain.WhatsappFarmSession;
import br.com.curralzap.backend.whatsapp.domain.WhatsappFarmSessionRepository;
import br.com.curralzap.backend.whatsapp.domain.WhatsappInboundMessage;
import br.com.curralzap.backend.whatsapp.domain.WhatsappOutboundMessage;
import br.com.curralzap.backend.whatsapp.domain.WhatsappOutboundStatus;
import br.com.curralzap.backend.whatsapp.domain.WhatsappProcessingStatus;
import br.com.curralzap.backend.whatsapp.domain.WhatsappProcessingStep;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WhatsappOutboundDeliveryServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-04-29T12:30:00Z"),
            ZoneOffset.UTC
    );

    @Test
    void sendsButtonsConfirmationForValidWeighingAndPreservesInboundCorrelation() {
        Fixture fixture = new Fixture();
        UUID farmId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        WhatsappInboundMessage inboundMessage = fixture.inboundMessage(
                "wamid.in.1",
                "5511999990001",
                farmId,
                userId,
                "corr-1",
                WhatsappProcessingStatus.AWAITING_CONFIRMATION
        );
        fixture.extractedPayload(inboundMessage, "register_weighing", """
                {"intent":"register_weighing","animalIdentifier":"Boi 347","weightKg":520,"confidence":0.94}
                """);
        fixture.gateway.nextProviderMessageId = "wamid.out.1";

        fixture.service.handleRequestedDelivery(eventFor(inboundMessage, WhatsappProcessingStatus.AWAITING_CONFIRMATION));

        assertThat(fixture.gateway.sentButtonMessages).singleElement()
                .satisfies(message -> {
                    assertThat(message.phoneNumber()).isEqualTo("5511999990001");
                    assertThat(message.quotedProviderMessageId()).isEqualTo("wamid.in.1");
                    assertThat(message.title()).isEqualTo("Registrar *520 kg* para o animal *Boi 347*?");
                    assertThat(message.description()).isEqualTo("⚖️ Pesagem");
                    assertThat(message.buttons()).hasSize(2);
                    assertThat(message.buttons().get(0).displayText()).isEqualTo("✅ Confirmar");
                    assertThat(message.buttons().get(0).id()).startsWith("confirm:");
                    assertThat(message.buttons().get(1).displayText()).isEqualTo("❌ Cancelar");
                });
        assertThat(fixture.outboundMessages()).singleElement()
                .satisfies(message -> {
                    assertThat(message.inboundMessageId()).isEqualTo(inboundMessage.id());
                    assertThat(message.farmId()).isEqualTo(farmId);
                    assertThat(message.userId()).isEqualTo(userId);
                    assertThat(message.correlationId()).isEqualTo("corr-1");
                    assertThat(message.status()).isEqualTo(WhatsappOutboundStatus.SENT);
                    assertThat(message.messageType()).isEqualTo("buttons");
                    assertThat(message.providerMessageId()).isEqualTo("wamid.out.1");
                    assertThat(message.payload()).contains("Registrar *520 kg* para o animal *Boi 347*?");
                });
    }

    @Test
    void sendsFriendlyPermissionDeniedErrorForRejectedPayloads() {
        Fixture fixture = new Fixture();
        WhatsappInboundMessage inboundMessage = fixture.inboundMessage(
                "wamid.in.2",
                "5511999990002",
                UUID.randomUUID(),
                UUID.randomUUID(),
                "corr-2",
                WhatsappProcessingStatus.REJECTED
        );
        fixture.processingStep(
                inboundMessage,
                WhatsappProcessingStatus.REJECTED,
                "Validation rejected permission denied for register_vaccine."
        );

        fixture.service.handleRequestedDelivery(eventFor(inboundMessage, WhatsappProcessingStatus.REJECTED));

        assertThat(fixture.gateway.sentMessages).singleElement()
                .extracting(WhatsappTextOutboundMessage::text)
                .isEqualTo("🚫 Você não tem permissão para registrar vacinas.\nPeça ao veterinário ou admin.");
    }

    @Test
    void sendsFriendlyUnknownIntentErrorForUnsupportedPayloads() {
        Fixture fixture = new Fixture();
        WhatsappInboundMessage inboundMessage = fixture.inboundMessage(
                "wamid.in.3",
                "5511999990003",
                UUID.randomUUID(),
                UUID.randomUUID(),
                "corr-3",
                WhatsappProcessingStatus.REJECTED
        );
        fixture.processingStep(
                inboundMessage,
                WhatsappProcessingStatus.REJECTED,
                "Validation rejected unsupported intent: unknown."
        );

        fixture.service.handleRequestedDelivery(eventFor(inboundMessage, WhatsappProcessingStatus.REJECTED));

        assertThat(fixture.gateway.sentMessages).singleElement()
                .extracting(WhatsappTextOutboundMessage::text)
                .isEqualTo("❓ Não entendi o comando.\nEnvie um áudio dizendo *\"ajuda\"* para ver o que posso fazer. 🎙️");
    }

    @Test
    void sendsPlanLimitErrorForRejectedPlanLimitPayloads() {
        Fixture fixture = new Fixture();
        WhatsappInboundMessage inboundMessage = fixture.inboundMessage(
                "wamid.in.4",
                "5511999990004",
                UUID.randomUUID(),
                UUID.randomUUID(),
                "corr-4",
                WhatsappProcessingStatus.REJECTED
        );
        fixture.processingStep(
                inboundMessage,
                WhatsappProcessingStatus.REJECTED,
                "Validation rejected plan limit: daily weighing limit reached."
        );

        fixture.service.handleRequestedDelivery(eventFor(inboundMessage, WhatsappProcessingStatus.REJECTED));

        assertThat(fixture.gateway.sentMessages).singleElement()
                .extracting(WhatsappTextOutboundMessage::text)
                .isEqualTo("📊 Você atingiu o limite diário do plano Free para pesagens.\nPara continuar, use o app ou faça upgrade. 🚀");
    }

    @Test
    void tracksOutboundFailuresWithoutLosingCorrelation() {
        Fixture fixture = new Fixture();
        UUID farmId = UUID.randomUUID();
        WhatsappInboundMessage inboundMessage = fixture.inboundMessage(
                "wamid.in.5",
                "5511999990005",
                farmId,
                null,
                "corr-5",
                WhatsappProcessingStatus.AWAITING_CONFIRMATION
        );
        fixture.extractedPayload(inboundMessage, "register_occurrence", """
                {"intent":"register_occurrence","animalIdentifier":"Boi 89","description":"mancando","severity":"ALTA","confidence":0.91}
                """);
        fixture.gateway.buttonsFailure = new WhatsappOutboundDeliveryException("Evolution 502");

        assertThatThrownBy(() -> fixture.service.handleRequestedDelivery(
                eventFor(inboundMessage, WhatsappProcessingStatus.AWAITING_CONFIRMATION)
        ))
                .isInstanceOf(WhatsappWorkerRetryableException.class)
                .hasMessage("Failed to process WhatsApp outbound delivery worker event.");

        assertThat(fixture.outboundMessages()).singleElement()
                .satisfies(message -> {
                    assertThat(message.inboundMessageId()).isEqualTo(inboundMessage.id());
                    assertThat(message.farmId()).isEqualTo(farmId);
                    assertThat(message.correlationId()).isEqualTo("corr-5");
                    assertThat(message.status()).isEqualTo(WhatsappOutboundStatus.FAILED);
                    assertThat(message.failureReason()).isEqualTo("Evolution 502");
                });
    }

    private static WhatsappOutboundDeliveryRequestedEvent eventFor(
            WhatsappInboundMessage inboundMessage,
            WhatsappProcessingStatus status
    ) {
        return new WhatsappOutboundDeliveryRequestedEvent(
                1,
                "whatsapp.outbound.delivery.requested.v1",
                UUID.randomUUID().toString(),
                inboundMessage.correlationId(),
                OffsetDateTime.now(FIXED_CLOCK),
                inboundMessage.id(),
                inboundMessage.providerMessageId(),
                status
        );
    }

    private static final class Fixture {

        private final InMemoryWhatsappInboundMessageRepository inboundMessageRepository =
                new InMemoryWhatsappInboundMessageRepository();
        private final InMemoryWhatsappExtractedPayloadRepository extractedPayloadRepository =
                new InMemoryWhatsappExtractedPayloadRepository();
        private final InMemoryWhatsappOutboundMessageRepository outboundMessageRepository =
                new InMemoryWhatsappOutboundMessageRepository();
        private final CapturingWhatsappOutboundGateway gateway = new CapturingWhatsappOutboundGateway();
        private final WhatsappOutboundDeliveryService service = new WhatsappOutboundDeliveryService(
                inboundMessageRepository,
                extractedPayloadRepository,
                outboundMessageRepository,
                gateway,
                new NoOpWhatsappFarmSessionRepository(),
                FIXED_CLOCK
        );

        private WhatsappInboundMessage inboundMessage(
                String providerMessageId,
                String phoneNumber,
                UUID farmId,
                UUID userId,
                String correlationId,
                WhatsappProcessingStatus status
        ) {
            WhatsappInboundMessage received = WhatsappInboundMessage.receive(
                    providerMessageId,
                    "messages.upsert",
                    phoneNumber,
                    farmId,
                    userId,
                    correlationId,
                    "conversation",
                    "{\"event\":\"messages.upsert\"}",
                    OffsetDateTime.now(FIXED_CLOCK).minusMinutes(3)
            );
            WhatsappInboundMessage saved = inboundMessageRepository.save(received);
            return inboundMessageRepository.recordProcessingStep(
                    saved.id(),
                    status,
                    status == WhatsappProcessingStatus.REJECTED ? "Validation rejected." : "Validation passed.",
                    OffsetDateTime.now(FIXED_CLOCK).minusMinutes(1)
            );
        }

        private void extractedPayload(WhatsappInboundMessage inboundMessage, String intent, String structuredPayload) {
            extractedPayloadRepository.save(new WhatsappExtractedPayload(
                    null,
                    inboundMessage.id(),
                    "openai",
                    "gpt-5.4-mini",
                    intent,
                    structuredPayload.strip(),
                    OffsetDateTime.now(FIXED_CLOCK).minusMinutes(2),
                    OffsetDateTime.now(FIXED_CLOCK).minusMinutes(2),
                    OffsetDateTime.now(FIXED_CLOCK).minusMinutes(2)
            ));
        }

        private void processingStep(
                WhatsappInboundMessage inboundMessage,
                WhatsappProcessingStatus status,
                String details
        ) {
            inboundMessageRepository.recordProcessingStep(
                    inboundMessage.id(),
                    status,
                    details,
                    OffsetDateTime.now(FIXED_CLOCK)
            );
        }

        private List<WhatsappOutboundMessage> outboundMessages() {
            return outboundMessageRepository.messagesById.values().stream()
                    .sorted(Comparator.comparing(WhatsappOutboundMessage::createdAt))
                    .toList();
        }
    }

    private static final class CapturingWhatsappOutboundGateway implements WhatsappOutboundGateway {

        private final List<WhatsappTextOutboundMessage> sentMessages = new ArrayList<>();
        private final List<WhatsappButtonsOutboundMessage> sentButtonMessages = new ArrayList<>();
        private String nextProviderMessageId = "wamid.out.default";
        private RuntimeException failure;
        private RuntimeException buttonsFailure;

        @Override
        public WhatsappOutboundDeliveryResult sendText(WhatsappTextOutboundMessage message) {
            sentMessages.add(message);
            if (failure != null) {
                throw failure;
            }
            return new WhatsappOutboundDeliveryResult(nextProviderMessageId);
        }

        @Override
        public WhatsappOutboundDeliveryResult sendButtons(WhatsappButtonsOutboundMessage message) {
            sentButtonMessages.add(message);
            if (buttonsFailure != null) {
                throw buttonsFailure;
            }
            return new WhatsappOutboundDeliveryResult(nextProviderMessageId);
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

    private static final class InMemoryWhatsappInboundMessageRepository implements WhatsappInboundMessageRepository {

        private final Map<UUID, WhatsappInboundMessage> messagesById = new HashMap<>();
        private final List<WhatsappProcessingStep> processingSteps = new ArrayList<>();

        @Override
        public WhatsappInboundMessage save(WhatsappInboundMessage message) {
            UUID id = message.id() == null ? UUID.randomUUID() : message.id();
            WhatsappInboundMessage persisted = new WhatsappInboundMessage(
                    id,
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
            );
            messagesById.put(id, persisted);
            processingSteps.add(new WhatsappProcessingStep(
                    UUID.randomUUID(),
                    id,
                    persisted.status(),
                    null,
                    persisted.receivedAt(),
                    persisted.createdAt(),
                    persisted.updatedAt()
            ));
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
                    .limit(limit)
                    .toList();
        }

        @Override
        public List<WhatsappProcessingStep> findProcessingSteps(UUID inboundMessageId) {
            return processingSteps.stream()
                    .filter(step -> inboundMessageId.equals(step.inboundMessageId()))
                    .sorted(Comparator.comparing(WhatsappProcessingStep::occurredAt))
                    .toList();
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
            processingSteps.add(new WhatsappProcessingStep(
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

    private static final class InMemoryWhatsappExtractedPayloadRepository implements WhatsappExtractedPayloadRepository {

        private final Map<UUID, WhatsappExtractedPayload> payloadsByInboundId = new HashMap<>();

        @Override
        public WhatsappExtractedPayload save(WhatsappExtractedPayload payload) {
            UUID id = payload.id() == null ? UUID.randomUUID() : payload.id();
            WhatsappExtractedPayload persisted = new WhatsappExtractedPayload(
                    id,
                    payload.inboundMessageId(),
                    payload.provider(),
                    payload.providerModel(),
                    payload.intent(),
                    payload.structuredPayload(),
                    payload.extractedAt(),
                    payload.createdAt(),
                    payload.updatedAt()
            );
            payloadsByInboundId.put(payload.inboundMessageId(), persisted);
            return persisted;
        }

        @Override
        public Optional<WhatsappExtractedPayload> findByInboundMessageId(UUID inboundMessageId) {
            return Optional.ofNullable(payloadsByInboundId.get(inboundMessageId));
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

    private static final class NoOpWhatsappFarmSessionRepository implements WhatsappFarmSessionRepository {
        @Override
        public Optional<WhatsappFarmSession> findByPhoneAndDate(String phoneNumber, LocalDate date) {
            return Optional.empty();
        }

        @Override
        public void save(String phoneNumber, UUID farmId, String farmName, LocalDate date) {}
    }
}
