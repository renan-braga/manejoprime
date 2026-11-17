package br.com.curralzap.backend.whatsapp.application;

import br.com.curralzap.backend.farms.domain.FarmMembershipDetails;
import br.com.curralzap.backend.farms.domain.UserFarmMembershipRepository;
import br.com.curralzap.backend.support.InMemoryUserAccountRepository;
import br.com.curralzap.backend.support.TestUserAccounts;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserRole;
import br.com.curralzap.backend.whatsapp.domain.WhatsappFarmSession;
import br.com.curralzap.backend.whatsapp.domain.WhatsappFarmSessionRepository;
import br.com.curralzap.backend.whatsapp.domain.WhatsappInboundMessage;
import br.com.curralzap.backend.whatsapp.domain.WhatsappProcessingStatus;
import br.com.curralzap.backend.whatsapp.domain.WhatsappProcessingStep;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WhatsappWebhookIntakeServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-04-03T18:30:00Z"), ZoneOffset.UTC);

    private static WhatsappWebhookIntakeService buildService(
            WhatsappInboundMessageRepository repository,
            WhatsappReceivedMessagePublisher publisher,
            WhatsappButtonConfirmationRequestedPublisher buttonPublisher,
            InMemoryUserAccountRepository userAccountRepository
    ) {
        WhatsappFarmSelectionService selectionService = new WhatsappFarmSelectionService(
                new NoOpWhatsappFarmSessionRepository(),
                new NoOpWhatsappOutboundGateway(),
                new ObjectMapper()
        );
        return new WhatsappWebhookIntakeService(
                repository, publisher, buttonPublisher, userAccountRepository,
                new NoOpUserFarmMembershipRepository(),
                selectionService,
                FIXED_CLOCK
        );
    }

    @Test
    void persistsReceivedMessageAndResolvesKnownUser() {
        InMemoryUserAccountRepository userAccountRepository = new InMemoryUserAccountRepository();
        InMemoryWhatsappInboundMessageRepository repository = new InMemoryWhatsappInboundMessageRepository();
        CapturingWhatsappReceivedMessagePublisher publisher = new CapturingWhatsappReceivedMessagePublisher();
        WhatsappWebhookIntakeService service = buildService(repository, publisher, event -> {}, userAccountRepository);
        UserAccount knownUser = TestUserAccounts.userAccount(
                "5511999990001",
                "Admin User",
                UserRole.ADMIN,
                true,
                OffsetDateTime.now(FIXED_CLOCK)
        );
        userAccountRepository.save(knownUser);

        service.intake(new WhatsappWebhookIntakeCommand(
                "messages.upsert",
                "wamid.ABC123",
                "5511999990001@s.whatsapp.net",
                "conversation",
                "{\"event\":\"messages.upsert\"}",
                null
        ), "req-wa-003-1");

        assertThat(repository.messages).hasSize(1);
        assertThat(repository.messages.getFirst())
                .satisfies(message -> {
                    assertThat(message.providerMessageId()).isEqualTo("wamid.ABC123");
                    assertThat(message.phoneNumber()).isEqualTo("5511999990001");
                    assertThat(message.userId()).isEqualTo(knownUser.id());
                    assertThat(message.farmId()).isEqualTo(knownUser.farmId());
                    assertThat(message.status()).isEqualTo(WhatsappProcessingStatus.RECEIVED);
                    assertThat(message.receivedAt()).isEqualTo(OffsetDateTime.now(FIXED_CLOCK));
                });
        assertThat(publisher.events).singleElement()
                .satisfies(event -> {
                    assertThat(event.correlationId()).isEqualTo("req-wa-003-1");
                    assertThat(event.providerMessageId()).isEqualTo("wamid.ABC123");
                    assertThat(event.eventType()).isEqualTo("whatsapp.message.received.v1");
                });
    }

    @Test
    void persistsReceivedMessageWithoutResolvedUserWhenPhoneIsUnknown() {
        InMemoryWhatsappInboundMessageRepository repository = new InMemoryWhatsappInboundMessageRepository();
        WhatsappWebhookIntakeService service = buildService(
                repository, new CapturingWhatsappReceivedMessagePublisher(), event -> {}, new InMemoryUserAccountRepository()
        );

        service.intake(new WhatsappWebhookIntakeCommand(
                "messages.upsert",
                "wamid.UNKNOWN1",
                "5511999990999@s.whatsapp.net",
                "audioMessage",
                "{\"event\":\"messages.upsert\"}",
                null
        ), "req-wa-003-2");

        assertThat(repository.messages).hasSize(1);
        assertThat(repository.messages.getFirst().userId()).isNull();
        assertThat(repository.messages.getFirst().farmId()).isNull();
    }

    @Test
    void rejectsMalformedSenderJid() {
        WhatsappWebhookIntakeService service = buildService(
                new InMemoryWhatsappInboundMessageRepository(),
                new CapturingWhatsappReceivedMessagePublisher(),
                event -> {},
                new InMemoryUserAccountRepository()
        );

        assertThatThrownBy(() -> service.intake(new WhatsappWebhookIntakeCommand(
                "messages.upsert",
                "wamid.BAD1",
                "grupo-123@g.us",
                "conversation",
                "{\"event\":\"messages.upsert\"}",
                null
        ), "req-wa-003-3"))
                .isInstanceOf(InvalidWhatsappWebhookPayloadException.class)
                .satisfies(exception -> assertThat(((InvalidWhatsappWebhookPayloadException) exception).fieldErrors())
                        .containsEntry("data.key.remoteJid", "Webhook sender must be a direct WhatsApp phone JID."));
    }

    @Test
    void persistsMessageBeforeThrowingExplicitPublishFailure() {
        InMemoryWhatsappInboundMessageRepository repository = new InMemoryWhatsappInboundMessageRepository();
        CapturingWhatsappReceivedMessagePublisher publisher = new CapturingWhatsappReceivedMessagePublisher();
        publisher.fail = true;
        WhatsappWebhookIntakeService service = buildService(repository, publisher, event -> {}, new InMemoryUserAccountRepository());

        assertThatThrownBy(() -> service.intake(new WhatsappWebhookIntakeCommand(
                "messages.upsert",
                "wamid.FAIL1",
                "5511999990001@s.whatsapp.net",
                "conversation",
                "{\"event\":\"messages.upsert\"}",
                null
        ), "req-wa-003-fail"))
                .isInstanceOf(WhatsappMessagePublishFailedException.class);

        assertThat(repository.messages).hasSize(1);
        assertThat(repository.messages.getFirst().status()).isEqualTo(WhatsappProcessingStatus.FAILED);
        assertThat(repository.messages.getFirst().failureReason())
                .isEqualTo("Received event publish failed: Kafka unavailable");
    }

    @Test
    void ignoresDuplicateWebhookWhenOriginalMessageWasAlreadyAccepted() {
        InMemoryWhatsappInboundMessageRepository repository = new InMemoryWhatsappInboundMessageRepository();
        CapturingWhatsappReceivedMessagePublisher publisher = new CapturingWhatsappReceivedMessagePublisher();
        WhatsappWebhookIntakeService service = buildService(repository, publisher, event -> {}, new InMemoryUserAccountRepository());
        WhatsappWebhookIntakeCommand command = new WhatsappWebhookIntakeCommand(
                "messages.upsert",
                "wamid.DUPLICATE1",
                "5511999990001@s.whatsapp.net",
                "conversation",
                "{\"event\":\"messages.upsert\"}",
                null
        );

        service.intake(command, "req-wa-010-first");
        service.intake(command, "req-wa-010-duplicate");

        assertThat(repository.messages).hasSize(1);
        assertThat(publisher.events).singleElement()
                .extracting(WhatsappMessageReceivedEvent::correlationId)
                .isEqualTo("req-wa-010-first");
    }

    @Test
    void retriesInitialPublishForDuplicateWebhookAfterPublishFailure() {
        InMemoryWhatsappInboundMessageRepository repository = new InMemoryWhatsappInboundMessageRepository();
        CapturingWhatsappReceivedMessagePublisher publisher = new CapturingWhatsappReceivedMessagePublisher();
        publisher.fail = true;
        WhatsappWebhookIntakeService service = buildService(repository, publisher, event -> {}, new InMemoryUserAccountRepository());
        WhatsappWebhookIntakeCommand command = new WhatsappWebhookIntakeCommand(
                "messages.upsert",
                "wamid.DUPLICATE2",
                "5511999990002@s.whatsapp.net",
                "conversation",
                "{\"event\":\"messages.upsert\"}",
                null
        );

        assertThatThrownBy(() -> service.intake(command, "req-wa-010-fail"))
                .isInstanceOf(WhatsappMessagePublishFailedException.class);

        publisher.fail = false;
        service.intake(command, "req-wa-010-retry");

        assertThat(repository.messages).hasSize(1);
        assertThat(repository.messages.getFirst().status()).isEqualTo(WhatsappProcessingStatus.RECEIVED);
        assertThat(repository.messages.getFirst().failureReason()).isNull();
        assertThat(publisher.events).singleElement()
                .extracting(WhatsappMessageReceivedEvent::correlationId)
                .isEqualTo("req-wa-010-retry");
    }

    private static final class InMemoryWhatsappInboundMessageRepository implements WhatsappInboundMessageRepository {

        private final List<WhatsappInboundMessage> messages = new ArrayList<>();
        private final List<WhatsappProcessingStep> processingSteps = new ArrayList<>();

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
            messages.add(persisted);
            processingSteps.add(new WhatsappProcessingStep(
                    UUID.randomUUID(),
                    persisted.id(),
                    persisted.status(),
                    null,
                    persisted.receivedAt(),
                    persisted.createdAt(),
                    persisted.updatedAt()
            ));
            return persisted;
        }

        @Override
        public Optional<WhatsappInboundMessage> findByProviderMessageId(String providerMessageId) {
            return messages.stream()
                    .filter(message -> message.providerMessageId().equals(providerMessageId))
                    .findFirst();
        }

        @Override
        public Optional<WhatsappInboundMessage> findById(UUID inboundMessageId) {
            return messages.stream().filter(message -> message.id().equals(inboundMessageId)).findFirst();
        }

        @Override
        public List<WhatsappInboundMessage> findRecentByFarmId(UUID farmId, int limit) {
            return messages.stream()
                    .filter(message -> farmId.equals(message.farmId()))
                    .sorted((left, right) -> right.createdAt().compareTo(left.createdAt()))
                    .limit(limit)
                    .toList();
        }

        @Override
        public List<WhatsappProcessingStep> findProcessingSteps(UUID inboundMessageId) {
            return processingSteps.stream()
                    .filter(step -> step.inboundMessageId().equals(inboundMessageId))
                    .toList();
        }

        @Override
        public WhatsappInboundMessage recordProcessingStep(
                UUID inboundMessageId,
                WhatsappProcessingStatus status,
                String details,
                OffsetDateTime occurredAt
        ) {
            WhatsappInboundMessage current = findById(inboundMessageId).orElseThrow();
            WhatsappInboundMessage updated = current.withProcessingStatus(status, details, occurredAt);
            messages.replaceAll(message -> message.id().equals(inboundMessageId) ? updated : message);
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

    private static final class CapturingWhatsappReceivedMessagePublisher implements WhatsappReceivedMessagePublisher {

        private final List<WhatsappMessageReceivedEvent> events = new ArrayList<>();
        private boolean fail;

        @Override
        public void publishReceived(WhatsappMessageReceivedEvent event) {
            if (fail) {
                throw new IllegalStateException("Kafka unavailable");
            }
            events.add(event);
        }
    }

    private static final class NoOpUserFarmMembershipRepository implements UserFarmMembershipRepository {
        @Override
        public void save(UUID userId, UUID farmId, String role) {}

        @Override
        public int countByUserId(UUID userId) { return 0; }

        @Override
        public boolean existsByUserIdAndFarmId(UUID userId, UUID farmId) { return false; }

        @Override
        public List<FarmMembershipDetails> findWithFarmNameByUserId(UUID userId) { return List.of(); }
    }

    private static final class NoOpWhatsappFarmSessionRepository implements WhatsappFarmSessionRepository {
        @Override
        public Optional<WhatsappFarmSession> findByPhoneAndDate(String phoneNumber, LocalDate date) {
            return Optional.empty();
        }

        @Override
        public void save(String phoneNumber, UUID farmId, String farmName, LocalDate date) {}
    }

    private static final class NoOpWhatsappOutboundGateway implements WhatsappOutboundGateway {
        @Override
        public WhatsappOutboundDeliveryResult sendText(WhatsappTextOutboundMessage message) { return null; }

        @Override
        public WhatsappOutboundDeliveryResult sendButtons(WhatsappButtonsOutboundMessage message) { return null; }
    }
}
