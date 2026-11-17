package br.com.curralzap.backend.whatsapp.application;

import br.com.curralzap.backend.farms.domain.FarmMembershipDetails;
import br.com.curralzap.backend.farms.domain.UserFarmMembershipRepository;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserAccountRepository;
import br.com.curralzap.backend.whatsapp.domain.WhatsappFarmSession;
import br.com.curralzap.backend.whatsapp.domain.WhatsappInboundMessage;
import br.com.curralzap.backend.whatsapp.domain.WhatsappProcessingStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class WhatsappWebhookIntakeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WhatsappWebhookIntakeService.class);
    private static final Pattern DIRECT_WHATSAPP_JID = Pattern.compile("^(\\d+)@s\\.whatsapp\\.net$");
    private static final String RECEIVED_EVENT_PUBLISH_FAILED_PREFIX = "Received event publish failed: ";

    private final WhatsappInboundMessageRepository repository;
    private final WhatsappReceivedMessagePublisher receivedMessagePublisher;
    private final WhatsappButtonConfirmationRequestedPublisher buttonConfirmationPublisher;
    private final UserAccountRepository userAccountRepository;
    private final UserFarmMembershipRepository membershipRepository;
    private final WhatsappFarmSelectionService farmSelectionService;
    private final Clock clock;

    @Autowired
    public WhatsappWebhookIntakeService(
            WhatsappInboundMessageRepository repository,
            WhatsappReceivedMessagePublisher receivedMessagePublisher,
            WhatsappButtonConfirmationRequestedPublisher buttonConfirmationPublisher,
            UserAccountRepository userAccountRepository,
            UserFarmMembershipRepository membershipRepository,
            WhatsappFarmSelectionService farmSelectionService
    ) {
        this(repository, receivedMessagePublisher, buttonConfirmationPublisher, userAccountRepository,
                membershipRepository, farmSelectionService, Clock.systemUTC());
    }

    WhatsappWebhookIntakeService(
            WhatsappInboundMessageRepository repository,
            WhatsappReceivedMessagePublisher receivedMessagePublisher,
            WhatsappButtonConfirmationRequestedPublisher buttonConfirmationPublisher,
            UserAccountRepository userAccountRepository,
            UserFarmMembershipRepository membershipRepository,
            WhatsappFarmSelectionService farmSelectionService,
            Clock clock
    ) {
        this.repository = repository;
        this.receivedMessagePublisher = receivedMessagePublisher;
        this.buttonConfirmationPublisher = buttonConfirmationPublisher;
        this.userAccountRepository = userAccountRepository;
        this.membershipRepository = membershipRepository;
        this.farmSelectionService = farmSelectionService;
        this.clock = clock;
    }

    public void intake(WhatsappWebhookIntakeCommand command, String correlationId) {
        String normalizedPhone = normalizeRemoteJid(command.remoteJid());

        if (command.selectedButtonId() != null) {
            LOGGER.info("Routing to button response handler messageType={} selectedButtonId={}", command.messageType(), command.selectedButtonId());
            handleButtonResponse(command, normalizedPhone, correlationId);
            return;
        }

        LOGGER.info("Routing to regular message handler messageType={}", command.messageType());

        Optional<WhatsappInboundMessage> existingMessage = repository.findByProviderMessageId(command.providerMessageId());
        if (existingMessage.isPresent()) {
            retryInitialPublishIfNeeded(existingMessage.get(), correlationId);
            return;
        }

        Optional<UserAccount> resolvedUser = userAccountRepository.findByPhone(normalizedPhone);
        OffsetDateTime now = OffsetDateTime.now(clock);

        UUID resolvedFarmId = resolvedUser.map(UserAccount::farmId).orElse(null);
        UUID resolvedUserId = resolvedUser.map(UserAccount::id).orElse(null);

        LOGGER.info("intake phone={} userFound={} resolvedUserId={} resolvedFarmId={}",
                normalizedPhone, resolvedUser.isPresent(), resolvedUserId, resolvedFarmId);

        if (resolvedUser.isPresent()) {
            List<FarmMembershipDetails> memberships = membershipRepository.findWithFarmNameByUserId(resolvedUserId);
            LOGGER.info("intake memberships.size={} for userId={}", memberships.size(), resolvedUserId);
            if (memberships.size() > 1) {
                LocalDate today = LocalDate.now(clock);
                Optional<WhatsappFarmSession> session = farmSelectionService.findActiveSession(normalizedPhone, today);
                LOGGER.info("intake multi-farm sessionFound={} sessionFarmId={} date={}",
                        session.isPresent(), session.map(WhatsappFarmSession::farmId).orElse(null), today);
                if (session.isPresent()) {
                    resolvedFarmId = session.get().farmId();
                    LOGGER.info("intake multi-farm session resolving to farmId={}", resolvedFarmId);
                } else {
                    WhatsappInboundMessage persisted = repository.save(WhatsappInboundMessage.receive(
                            command.providerMessageId(), command.eventType(), normalizedPhone,
                            null, resolvedUserId, correlationId, command.messageType(), command.rawPayload(), now
                    ));
                    Optional<FarmMembershipDetails> selected = farmSelectionService.parseSelection(command.rawPayload(), memberships);
                    if (selected.isPresent()) {
                        LOGGER.info("intake farm-selected farmId={}", selected.get().farmId());
                        farmSelectionService.saveSession(normalizedPhone, selected.get().farmId(), selected.get().farmName(), today);
                        farmSelectionService.sendSelectionConfirmation(normalizedPhone, selected.get().farmName(), command.providerMessageId());
                        repository.recordProcessingStep(persisted.id(), WhatsappProcessingStatus.FARM_SELECTED, null, now);
                    } else {
                        LOGGER.info("intake pending-farm-selection sending prompt to phone={}", normalizedPhone);
                        farmSelectionService.sendSelectionPrompt(normalizedPhone, memberships, command.providerMessageId());
                        repository.recordProcessingStep(persisted.id(), WhatsappProcessingStatus.PENDING_FARM_SELECTION, null, now);
                        LOGGER.info("intake pending-farm-selection recorded for messageId={}", persisted.id());
                    }
                    return;
                }
            } else if (memberships.size() == 1) {
                resolvedFarmId = memberships.get(0).farmId();
            }
        }

        WhatsappInboundMessage persistedMessage = repository.save(WhatsappInboundMessage.receive(
                command.providerMessageId(),
                command.eventType(),
                normalizedPhone,
                resolvedFarmId,
                resolvedUserId,
                correlationId,
                command.messageType(),
                command.rawPayload(),
                now
        ));

        publishReceivedMessage(persistedMessage, correlationId);
    }

    private void handleButtonResponse(WhatsappWebhookIntakeCommand command, String normalizedPhone, String correlationId) {
        if (repository.findByProviderMessageId(command.providerMessageId()).isPresent()) {
            return;
        }

        Optional<UserAccount> resolvedUser = userAccountRepository.findByPhone(normalizedPhone);
        OffsetDateTime now = OffsetDateTime.now(clock);

        WhatsappInboundMessage buttonMessage = repository.save(WhatsappInboundMessage.receive(
                command.providerMessageId(),
                command.eventType(),
                normalizedPhone,
                resolvedUser.map(UserAccount::farmId).orElse(null),
                resolvedUser.map(UserAccount::id).orElse(null),
                correlationId,
                command.messageType(),
                command.rawPayload(),
                now
        ));

        try {
            buttonConfirmationPublisher.publishRequested(new WhatsappButtonConfirmationRequestedEvent(
                    1,
                    "whatsapp.button.confirmation.requested.v1",
                    buttonMessage.id().toString(),
                    correlationId,
                    now,
                    buttonMessage.id(),
                    buttonMessage.providerMessageId(),
                    command.selectedButtonId()
            ));
        } catch (RuntimeException exception) {
            throw new WhatsappButtonConfirmationPublishFailedException(
                    buttonMessage.id(),
                    buttonMessage.providerMessageId(),
                    exception
            );
        }
    }

    private void retryInitialPublishIfNeeded(WhatsappInboundMessage existingMessage, String correlationId) {
        if (!isWaitingForInitialPublishRetry(existingMessage)) {
            return;
        }

        publishReceivedMessage(existingMessage, correlationId);
        repository.recordProcessingStep(
                existingMessage.id(),
                WhatsappProcessingStatus.RECEIVED,
                null,
                OffsetDateTime.now(clock)
        );
    }

    private boolean isWaitingForInitialPublishRetry(WhatsappInboundMessage existingMessage) {
        return existingMessage.status() == WhatsappProcessingStatus.FAILED
                && existingMessage.failureReason() != null
                && existingMessage.failureReason().startsWith(RECEIVED_EVENT_PUBLISH_FAILED_PREFIX);
    }

    private void publishReceivedMessage(WhatsappInboundMessage message, String correlationId) {
        try {
            receivedMessagePublisher.publishReceived(new WhatsappMessageReceivedEvent(
                    1,
                    "whatsapp.message.received.v1",
                    message.id().toString(),
                    correlationId,
                    message.receivedAt(),
                    message.id(),
                    message.providerMessageId(),
                    message.farmId(),
                    message.userId(),
                    message.phoneNumber(),
                    message.messageType(),
                    message.status()
            ));
        } catch (RuntimeException exception) {
            repository.recordProcessingStep(
                    message.id(),
                    WhatsappProcessingStatus.FAILED,
                    RECEIVED_EVENT_PUBLISH_FAILED_PREFIX + failureReason(exception),
                    OffsetDateTime.now(clock)
            );
            throw new WhatsappMessagePublishFailedException(
                    message.id(),
                    message.providerMessageId(),
                    exception
            );
        }
    }

    private String failureReason(RuntimeException exception) {
        Throwable cause = exception.getCause();
        if (cause != null && cause.getMessage() != null && !cause.getMessage().isBlank()) {
            return cause.getMessage();
        }
        if (exception.getMessage() != null && !exception.getMessage().isBlank()) {
            return exception.getMessage();
        }
        return exception.getClass().getSimpleName();
    }

    private String normalizeRemoteJid(String remoteJid) {
        Matcher matcher = DIRECT_WHATSAPP_JID.matcher(remoteJid);
        if (!matcher.matches()) {
            throw new InvalidWhatsappWebhookPayloadException(
                    Map.of("data.key.remoteJid", "Webhook sender must be a direct WhatsApp phone JID.")
            );
        }
        return matcher.group(1);
    }
}
