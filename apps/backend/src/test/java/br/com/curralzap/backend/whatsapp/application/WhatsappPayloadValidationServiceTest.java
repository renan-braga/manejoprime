package br.com.curralzap.backend.whatsapp.application;

import br.com.curralzap.backend.animals.domain.Animal;
import br.com.curralzap.backend.animals.domain.AnimalRepository;
import br.com.curralzap.backend.animals.domain.AnimalSex;
import br.com.curralzap.backend.animals.domain.AnimalStatus;
import br.com.curralzap.backend.billing.application.BillingAccessDeniedException;
import br.com.curralzap.backend.billing.application.BillingAccessPolicy;
import br.com.curralzap.backend.billing.application.BillingSnapshot;
import br.com.curralzap.backend.lots.domain.Lot;
import br.com.curralzap.backend.lots.domain.LotRepository;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserAccountRepository;
import br.com.curralzap.backend.users.domain.UserRole;
import br.com.curralzap.backend.whatsapp.domain.WhatsappExtractedPayload;
import br.com.curralzap.backend.whatsapp.domain.WhatsappInboundMessage;
import br.com.curralzap.backend.whatsapp.domain.WhatsappProcessingStatus;
import br.com.curralzap.backend.whatsapp.domain.WhatsappProcessingStep;
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

class WhatsappPayloadValidationServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-04-06T10:30:00Z"), ZoneOffset.UTC);

    @Test
    void rejectsUnauthorizedPhoneSafely() {
        TestFixture fixture = new TestFixture();
        WhatsappPayloadValidationService service = fixture.service();
        WhatsappInboundMessage inboundMessage = fixture.intentExtractedMessage(
                "wamid.validation-unauthorized",
                "5511999990601",
                null,
                null
        );
        fixture.extractedPayload(inboundMessage, "register_weighing", """
                {"intent":"register_weighing","animalIdentifier":"A-1","weightKg":324.5,"confidence":0.92}
                """);

        service.handleRequestedValidation(eventFor(inboundMessage, "corr-wa-008-unauthorized"));

        assertThat(fixture.inboundMessages.findByProviderMessageId("wamid.validation-unauthorized"))
                .get()
                .extracting(WhatsappInboundMessage::status)
                .isEqualTo(WhatsappProcessingStatus.REJECTED);
        assertThat(fixture.inboundMessages.findProcessingSteps(inboundMessage.id()))
                .last()
                .satisfies(step -> {
                    assertThat(step.status()).isEqualTo(WhatsappProcessingStatus.REJECTED);
                    assertThat(step.details()).isEqualTo("Validation rejected unauthorized phone number.");
                });
        assertThat(fixture.outboundDeliveryPublisher.events).singleElement()
                .satisfies(event -> {
                    assertThat(event.inboundMessageId()).isEqualTo(inboundMessage.id());
                    assertThat(event.providerMessageId()).isEqualTo("wamid.validation-unauthorized");
                    assertThat(event.status()).isEqualTo(WhatsappProcessingStatus.REJECTED);
                });
    }

    @Test
    void rejectsPermissionDeniedIntent() {
        TestFixture fixture = new TestFixture();
        UUID farmId = UUID.randomUUID();
        UserAccount worker = fixture.user("5511999990602", farmId, UserRole.WORKER, true);
        WhatsappPayloadValidationService service = fixture.service();
        WhatsappInboundMessage inboundMessage = fixture.intentExtractedMessage(
                "wamid.validation-permission",
                worker.phone(),
                farmId,
                worker.id()
        );
        fixture.extractedPayload(inboundMessage, "register_feeding", """
                {"intent":"register_feeding","animalIdentifier":"A-2","quantityKg":50,"confidence":0.91}
                """);

        service.handleRequestedValidation(eventFor(inboundMessage, "corr-wa-008-permission"));

        assertThat(fixture.inboundMessages.findByProviderMessageId("wamid.validation-permission"))
                .get()
                .extracting(WhatsappInboundMessage::status)
                .isEqualTo(WhatsappProcessingStatus.REJECTED);
        assertThat(fixture.inboundMessages.findProcessingSteps(inboundMessage.id()))
                .last()
                .extracting(WhatsappProcessingStep::details)
                .isEqualTo("Validation rejected permission denied for register_feeding.");
    }

    @Test
    void rejectsPlanLimitDenial() {
        TestFixture fixture = new TestFixture();
        UUID farmId = UUID.randomUUID();
        UserAccount admin = fixture.user("5511999990603", farmId, UserRole.ADMIN, true);
        WhatsappInboundMessage inboundMessage = fixture.intentExtractedMessage(
                "wamid.validation-plan",
                admin.phone(),
                farmId,
                admin.id()
        );
        fixture.animal(farmId, "A-3");
        fixture.extractedPayload(inboundMessage, "register_weighing", """
                {"intent":"register_weighing","animalIdentifier":"A-3","weightKg":300,"confidence":0.91}
                """);
        fixture.planLimitPolicy.result = WhatsappPlanLimitResult.denied("Daily WhatsApp weighing limit reached.");

        fixture.service().handleRequestedValidation(eventFor(inboundMessage, "corr-wa-008-plan"));

        assertThat(fixture.inboundMessages.findByProviderMessageId("wamid.validation-plan"))
                .get()
                .extracting(WhatsappInboundMessage::status)
                .isEqualTo(WhatsappProcessingStatus.REJECTED);
        assertThat(fixture.inboundMessages.findProcessingSteps(inboundMessage.id()))
                .last()
                .extracting(WhatsappProcessingStep::details)
                .isEqualTo("Validation rejected plan limit: Daily WhatsApp weighing limit reached.");
    }

    @Test
    void rejectsBillingDeniedFarmBeforePlanLimitCheck() {
        TestFixture fixture = new TestFixture();
        UUID farmId = UUID.randomUUID();
        UserAccount admin = fixture.user("5511999990607", farmId, UserRole.ADMIN, true);
        WhatsappInboundMessage inboundMessage = fixture.intentExtractedMessage(
                "wamid.validation-billing",
                admin.phone(),
                farmId,
                admin.id()
        );
        fixture.animal(farmId, "A-7");
        fixture.extractedPayload(inboundMessage, "register_weighing", """
                {"intent":"register_weighing","animalIdentifier":"A-7","weightKg":300,"confidence":0.91}
                """);
        fixture.billingAccessPolicy.snapshot = new BillingSnapshot(
                "pro",
                "suspended",
                null,
                null,
                false,
                null,
                false,
                false
        );

        fixture.service().handleRequestedValidation(eventFor(inboundMessage, "corr-wa-008-billing"));

        assertThat(fixture.inboundMessages.findByProviderMessageId("wamid.validation-billing"))
                .get()
                .extracting(WhatsappInboundMessage::status)
                .isEqualTo(WhatsappProcessingStatus.REJECTED);
        assertThat(fixture.inboundMessages.findProcessingSteps(inboundMessage.id()))
                .last()
                .extracting(WhatsappProcessingStep::details)
                .isEqualTo("Validation rejected billing status: suspended.");
        assertThat(fixture.planLimitPolicy.requests).isEmpty();
    }

    @Test
    void rejectsMissingReferencedAnimal() {
        TestFixture fixture = new TestFixture();
        UUID farmId = UUID.randomUUID();
        UserAccount veterinarian = fixture.user("5511999990604", farmId, UserRole.VETERINARIAN, true);
        WhatsappInboundMessage inboundMessage = fixture.intentExtractedMessage(
                "wamid.validation-missing-animal",
                veterinarian.phone(),
                farmId,
                veterinarian.id()
        );
        fixture.extractedPayload(inboundMessage, "register_vaccine", """
                {"intent":"register_vaccine","animalIdentifier":"A-404","vaccineName":"Brucelose","confidence":0.91}
                """);

        fixture.service().handleRequestedValidation(eventFor(inboundMessage, "corr-wa-008-missing"));

        assertThat(fixture.inboundMessages.findByProviderMessageId("wamid.validation-missing-animal"))
                .get()
                .extracting(WhatsappInboundMessage::status)
                .isEqualTo(WhatsappProcessingStatus.REJECTED);
        assertThat(fixture.inboundMessages.findProcessingSteps(inboundMessage.id()))
                .last()
                .extracting(WhatsappProcessingStep::details)
                .isEqualTo("Validation rejected missing animal A-404.");
    }

    @Test
    void validCommandAdvancesToAwaitingConfirmation() {
        TestFixture fixture = new TestFixture();
        UUID farmId = UUID.randomUUID();
        UserAccount worker = fixture.user("5511999990605", farmId, UserRole.WORKER, true);
        WhatsappInboundMessage inboundMessage = fixture.intentExtractedMessage(
                "wamid.validation-ok",
                worker.phone(),
                farmId,
                worker.id()
        );
        fixture.animal(farmId, "A-5");
        fixture.extractedPayload(inboundMessage, "register_occurrence", """
                {"intent":"register_occurrence","animalIdentifier":"A-5","description":"Animal mancando","confidence":0.91}
                """);

        fixture.service().handleRequestedValidation(eventFor(inboundMessage, "corr-wa-008-ok"));

        assertThat(fixture.planLimitPolicy.requests).singleElement().satisfies(request -> {
            assertThat(request.farmId()).isEqualTo(farmId);
            assertThat(request.userId()).isEqualTo(worker.id());
            assertThat(request.intent()).isEqualTo("register_occurrence");
            assertThat(request.occurredAt()).isEqualTo(OffsetDateTime.now(FIXED_CLOCK));
        });
        assertThat(fixture.inboundMessages.findByProviderMessageId("wamid.validation-ok"))
                .get()
                .extracting(WhatsappInboundMessage::status)
                .isEqualTo(WhatsappProcessingStatus.AWAITING_CONFIRMATION);
        assertThat(fixture.inboundMessages.findProcessingSteps(inboundMessage.id()))
                .extracting(WhatsappProcessingStep::status, WhatsappProcessingStep::details)
                .contains(
                        org.assertj.core.groups.Tuple.tuple(
                                WhatsappProcessingStatus.VALIDATED,
                                "Payload validated for register_occurrence."
                        ),
                        org.assertj.core.groups.Tuple.tuple(
                                WhatsappProcessingStatus.AWAITING_CONFIRMATION,
                                "Validation passed; awaiting confirmation for register_occurrence."
                        )
                );
        assertThat(fixture.outboundDeliveryPublisher.events).singleElement()
                .satisfies(event -> {
                    assertThat(event.inboundMessageId()).isEqualTo(inboundMessage.id());
                    assertThat(event.providerMessageId()).isEqualTo("wamid.validation-ok");
                    assertThat(event.status()).isEqualTo(WhatsappProcessingStatus.AWAITING_CONFIRMATION);
                });
    }

    @Test
    void retriesOutboundPublishWhenValidationAlreadyReachedAwaitingConfirmation() {
        TestFixture fixture = new TestFixture();
        UUID farmId = UUID.randomUUID();
        UserAccount worker = fixture.user("5511999990606", farmId, UserRole.WORKER, true);
        WhatsappInboundMessage inboundMessage = fixture.intentExtractedMessage(
                "wamid.validation-retry",
                worker.phone(),
                farmId,
                worker.id()
        );
        fixture.animal(farmId, "A-6");
        fixture.extractedPayload(inboundMessage, "register_occurrence", """
                {"intent":"register_occurrence","animalIdentifier":"A-6","description":"Animal isolado","confidence":0.91}
                """);
        fixture.outboundDeliveryPublisher.fail = true;

        assertThatThrownBy(() -> fixture.service().handleRequestedValidation(eventFor(inboundMessage, "corr-wa-011-retry-1")))
                .isInstanceOf(WhatsappOutboundDeliveryPublishFailedException.class);

        fixture.outboundDeliveryPublisher.fail = false;

        fixture.service().handleRequestedValidation(eventFor(inboundMessage, "corr-wa-011-retry-2"));

        assertThat(fixture.inboundMessages.findByProviderMessageId("wamid.validation-retry"))
                .get()
                .extracting(WhatsappInboundMessage::status)
                .isEqualTo(WhatsappProcessingStatus.AWAITING_CONFIRMATION);
        assertThat(fixture.outboundDeliveryPublisher.events).singleElement()
                .satisfies(deliveryEvent -> {
                    assertThat(deliveryEvent.inboundMessageId()).isEqualTo(inboundMessage.id());
                    assertThat(deliveryEvent.providerMessageId()).isEqualTo("wamid.validation-retry");
                    assertThat(deliveryEvent.status()).isEqualTo(WhatsappProcessingStatus.AWAITING_CONFIRMATION);
                });
    }

    private static WhatsappPayloadValidationRequestedEvent eventFor(
            WhatsappInboundMessage inboundMessage,
            String correlationId
    ) {
        return new WhatsappPayloadValidationRequestedEvent(
                1,
                "whatsapp.payload.validation.requested.v1",
                inboundMessage.id().toString(),
                correlationId,
                OffsetDateTime.parse("2026-04-06T10:20:00Z"),
                inboundMessage.id(),
                inboundMessage.providerMessageId()
        );
    }

    private static final class TestFixture {

        private final InMemoryWhatsappInboundMessageRepository inboundMessages =
                new InMemoryWhatsappInboundMessageRepository();
        private final InMemoryWhatsappExtractedPayloadRepository extractedPayloads =
                new InMemoryWhatsappExtractedPayloadRepository();
        private final InMemoryUserAccountRepository users = new InMemoryUserAccountRepository();
        private final InMemoryAnimalRepository animals = new InMemoryAnimalRepository();
        private final InMemoryLotRepository lots = new InMemoryLotRepository();
        private final CapturingWhatsappPlanLimitPolicy planLimitPolicy = new CapturingWhatsappPlanLimitPolicy();
        private final ConfigurableBillingAccessPolicy billingAccessPolicy = new ConfigurableBillingAccessPolicy();
        private final CapturingWhatsappOutboundDeliveryRequestedPublisher outboundDeliveryPublisher =
                new CapturingWhatsappOutboundDeliveryRequestedPublisher();

        private WhatsappPayloadValidationService service() {
            return new WhatsappPayloadValidationService(
                    inboundMessages,
                    extractedPayloads,
                    users,
                    animals,
                    lots,
                    planLimitPolicy,
                    billingAccessPolicy,
                    outboundDeliveryPublisher,
                    new com.fasterxml.jackson.databind.ObjectMapper(),
                    FIXED_CLOCK
            );
        }

        private UserAccount user(String phone, UUID farmId, UserRole role, boolean active) {
            UserAccount user = new UserAccount(
                    UUID.randomUUID(),
                    farmId,
                    phone,
                    null,
                    "User " + phone,
                    role,
                    active,
                    OffsetDateTime.parse("2026-04-06T09:00:00Z"),
                    OffsetDateTime.parse("2026-04-06T09:00:00Z")
            );
            users.save(user);
            return user;
        }

        private Animal animal(UUID farmId, String identifier) {
            return animals.save(new Animal(
                    UUID.randomUUID(),
                    farmId,
                    null,
                    identifier,
                    null,
                    null,
                    AnimalSex.MALE,
                    null,
                    null,
                    null,
                    null,
                    AnimalStatus.ACTIVE,
                    null,
                    OffsetDateTime.parse("2026-04-06T09:00:00Z"),
                    OffsetDateTime.parse("2026-04-06T09:00:00Z")
            ));
        }

        private WhatsappInboundMessage intentExtractedMessage(
                String providerMessageId,
                String phoneNumber,
                UUID farmId,
                UUID userId
        ) {
            WhatsappInboundMessage inboundMessage = inboundMessages.save(WhatsappInboundMessage.receive(
                    providerMessageId,
                    "messages.upsert",
                    phoneNumber,
                    farmId,
                    userId,
                    "corr-" + providerMessageId,
                    "audioMessage",
                    "{\"event\":\"messages.upsert\"}",
                    OffsetDateTime.parse("2026-04-06T10:00:00Z")
            ));
            inboundMessages.recordProcessingStep(
                    inboundMessage.id(),
                    WhatsappProcessingStatus.INTENT_EXTRACTED,
                    "Transcript extracted.",
                    OffsetDateTime.parse("2026-04-06T10:10:00Z")
            );
            return inboundMessages.findByProviderMessageId(providerMessageId).orElseThrow();
        }

        private void extractedPayload(WhatsappInboundMessage inboundMessage, String intent, String structuredPayload) {
            extractedPayloads.save(WhatsappExtractedPayload.extracted(
                    inboundMessage.id(),
                    "openai",
                    "gpt-5.4-mini",
                    intent,
                    structuredPayload.strip(),
                    OffsetDateTime.parse("2026-04-06T10:10:00Z")
            ));
        }
    }

    private static final class ConfigurableBillingAccessPolicy implements BillingAccessPolicy {

        private BillingSnapshot snapshot = new BillingSnapshot("free", "free", null, null, false, null, true, true);

        @Override
        public BillingSnapshot snapshotFor(UUID farmId) {
            return snapshot;
        }

        @Override
        public void requireWriteAllowed(UUID farmId) {
            if (!snapshot.canWrite()) {
                throw new BillingAccessDeniedException(snapshot);
            }
        }

        @Override
        public void requireWhatsappAllowed(UUID farmId) {
            if (!snapshot.canUseWhatsapp()) {
                throw new BillingAccessDeniedException(snapshot);
            }
        }
    }

    private static final class CapturingWhatsappPlanLimitPolicy implements WhatsappPlanLimitPolicy {

        private final List<WhatsappPlanLimitRequest> requests = new ArrayList<>();
        private WhatsappPlanLimitResult result = WhatsappPlanLimitResult.allowed();

        @Override
        public WhatsappPlanLimitResult check(WhatsappPlanLimitRequest request) {
            requests.add(request);
            return result;
        }
    }

    private static final class CapturingWhatsappOutboundDeliveryRequestedPublisher
            implements WhatsappOutboundDeliveryRequestedPublisher {

        private final List<WhatsappOutboundDeliveryRequestedEvent> events = new ArrayList<>();
        private boolean fail;

        @Override
        public void publishRequested(WhatsappOutboundDeliveryRequestedEvent event) {
            if (fail) {
                throw new WhatsappOutboundDeliveryPublishFailedException(
                        event.inboundMessageId(),
                        event.providerMessageId(),
                        new IllegalStateException("Kafka unavailable.")
                );
            }
            events.add(event);
        }
    }

    private static final class InMemoryUserAccountRepository implements UserAccountRepository {

        private final Map<UUID, UserAccount> usersById = new LinkedHashMap<>();

        @Override
        public Optional<UserAccount> findByPhone(String normalizedPhone) {
            return usersById.values().stream()
                    .filter(user -> user.phone().equals(normalizedPhone) || user.phone().equals("+" + normalizedPhone))
                    .findFirst();
        }

        @Override
        public Optional<UserAccount> findByEmail(String email) {
            return usersById.values().stream()
                    .filter(user -> email.equals(user.email()))
                    .findFirst();
        }

        @Override
        public Optional<UserAccount> findById(UUID userId) {
            return Optional.ofNullable(usersById.get(userId));
        }

        @Override
        public Optional<UserAccount> findByIdAndFarmId(UUID entityId, UUID farmId) {
            return Optional.ofNullable(usersById.get(entityId))
                    .filter(user -> user.farmId().equals(farmId));
        }

        @Override
        public UserAccount save(UserAccount userAccount) {
            usersById.put(userAccount.id(), userAccount);
            return userAccount;
        }
    }

    private static final class InMemoryAnimalRepository implements AnimalRepository {

        private final Map<UUID, Animal> animalsById = new LinkedHashMap<>();

        @Override
        public Animal save(Animal animal) {
            animalsById.put(animal.id(), animal);
            return animal;
        }

        @Override
        public Optional<Animal> findByFarmIdAndIdentifier(UUID farmId, String identifier) {
            return animalsById.values().stream()
                    .filter(animal -> animal.farmId().equals(farmId) && animal.identifier().equals(identifier))
                    .findFirst();
        }

        @Override
        public boolean existsByFarmIdAndIdentifier(UUID farmId, String identifier) {
            return animalsById.values().stream()
                    .anyMatch(animal -> animal.farmId().equals(farmId) && animal.identifier().equals(identifier));
        }

        @Override
        public boolean existsByFarmIdAndIdentifierAndIdNot(UUID farmId, String identifier, UUID animalId) {
            return animalsById.values().stream()
                    .anyMatch(animal -> animal.farmId().equals(farmId)
                            && animal.identifier().equals(identifier)
                            && !animal.id().equals(animalId));
        }

        @Override
        public void delete(UUID animalId) {
            animalsById.remove(animalId);
        }

        @Override
        public Optional<Animal> findByIdAndFarmId(UUID entityId, UUID farmId) {
            return Optional.ofNullable(animalsById.get(entityId))
                    .filter(animal -> animal.farmId().equals(farmId));
        }

        @Override
        public long countByFarmId(UUID farmId) {
            return animalsById.values().stream().filter(a -> a.farmId().equals(farmId)).count();
        }

        @Override
        public boolean existsByFarmId(UUID farmId) {
            return animalsById.values().stream().anyMatch(a -> a.farmId().equals(farmId));
        }
    }

    private static final class InMemoryLotRepository implements LotRepository {

        private final Map<UUID, Lot> lotsById = new LinkedHashMap<>();

        @Override
        public Lot save(Lot lot) {
            lotsById.put(lot.id(), lot);
            return lot;
        }

        @Override
        public Optional<Lot> findByIdAndFarmId(UUID entityId, UUID farmId) {
            return Optional.ofNullable(lotsById.get(entityId))
                    .filter(lot -> lot.farmId().equals(farmId));
        }

        @Override
        public boolean existsByFarmId(UUID farmId) {
            return lotsById.values().stream().anyMatch(l -> l.farmId().equals(farmId));
        }

        @Override
        public boolean existsByFarmIdAndName(UUID farmId, String name) {
            return lotsById.values().stream()
                    .anyMatch(l -> l.farmId().equals(farmId) && l.name().equals(name));
        }

        @Override
        public Optional<Lot> findByFarmIdAndName(UUID farmId, String name) {
            return lotsById.values().stream()
                    .filter(l -> l.farmId().equals(farmId) && l.name().equals(name))
                    .findFirst();
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
                    status == WhatsappProcessingStatus.FAILED || status == WhatsappProcessingStatus.REJECTED
                            ? details
                            : null,
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
