package br.com.curralzap.backend.whatsapp.application;

import br.com.curralzap.backend.animals.domain.AnimalRepository;
import br.com.curralzap.backend.auth.application.RolePermissionSummary;
import br.com.curralzap.backend.billing.application.BillingAccessDeniedException;
import br.com.curralzap.backend.billing.application.BillingAccessPolicy;
import br.com.curralzap.backend.lots.domain.LotRepository;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserAccountRepository;
import br.com.curralzap.backend.whatsapp.domain.WhatsappExtractedPayload;
import br.com.curralzap.backend.whatsapp.domain.WhatsappInboundMessage;
import br.com.curralzap.backend.whatsapp.domain.WhatsappProcessingStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class WhatsappPayloadValidationService {

    private static final Logger log = LoggerFactory.getLogger(WhatsappPayloadValidationService.class);
    private static final TypeReference<Map<String, Object>> PAYLOAD_MAP = new TypeReference<>() {
    };

    private final WhatsappInboundMessageRepository inboundMessageRepository;
    private final WhatsappExtractedPayloadRepository extractedPayloadRepository;
    private final UserAccountRepository userAccountRepository;
    private final AnimalRepository animalRepository;
    private final LotRepository lotRepository;
    private final WhatsappPlanLimitPolicy planLimitPolicy;
    private final BillingAccessPolicy billingAccessPolicy;
    private final WhatsappOutboundDeliveryRequestedPublisher outboundDeliveryRequestedPublisher;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public WhatsappPayloadValidationService(
            WhatsappInboundMessageRepository inboundMessageRepository,
            WhatsappExtractedPayloadRepository extractedPayloadRepository,
            UserAccountRepository userAccountRepository,
            AnimalRepository animalRepository,
            LotRepository lotRepository,
            WhatsappPlanLimitPolicy planLimitPolicy,
            BillingAccessPolicy billingAccessPolicy,
            WhatsappOutboundDeliveryRequestedPublisher outboundDeliveryRequestedPublisher,
            ObjectMapper objectMapper
    ) {
        this(
                inboundMessageRepository,
                extractedPayloadRepository,
                userAccountRepository,
                animalRepository,
                lotRepository,
                planLimitPolicy,
                billingAccessPolicy,
                outboundDeliveryRequestedPublisher,
                objectMapper,
                Clock.systemUTC()
        );
    }

    WhatsappPayloadValidationService(
            WhatsappInboundMessageRepository inboundMessageRepository,
            WhatsappExtractedPayloadRepository extractedPayloadRepository,
            UserAccountRepository userAccountRepository,
            AnimalRepository animalRepository,
            LotRepository lotRepository,
            WhatsappPlanLimitPolicy planLimitPolicy,
            Clock clock
    ) {
        this(
                inboundMessageRepository,
                extractedPayloadRepository,
                userAccountRepository,
                animalRepository,
                lotRepository,
                planLimitPolicy,
                BillingAccessPolicy.alwaysAllow(),
                event -> {
                },
                new ObjectMapper(),
                clock
        );
    }

    WhatsappPayloadValidationService(
            WhatsappInboundMessageRepository inboundMessageRepository,
            WhatsappExtractedPayloadRepository extractedPayloadRepository,
            UserAccountRepository userAccountRepository,
            AnimalRepository animalRepository,
            LotRepository lotRepository,
            WhatsappPlanLimitPolicy planLimitPolicy,
            WhatsappOutboundDeliveryRequestedPublisher outboundDeliveryRequestedPublisher,
            Clock clock
    ) {
        this(
                inboundMessageRepository,
                extractedPayloadRepository,
                userAccountRepository,
                animalRepository,
                lotRepository,
                planLimitPolicy,
                BillingAccessPolicy.alwaysAllow(),
                outboundDeliveryRequestedPublisher,
                new ObjectMapper(),
                clock
        );
    }

    WhatsappPayloadValidationService(
            WhatsappInboundMessageRepository inboundMessageRepository,
            WhatsappExtractedPayloadRepository extractedPayloadRepository,
            UserAccountRepository userAccountRepository,
            AnimalRepository animalRepository,
            LotRepository lotRepository,
            WhatsappPlanLimitPolicy planLimitPolicy,
            BillingAccessPolicy billingAccessPolicy,
            WhatsappOutboundDeliveryRequestedPublisher outboundDeliveryRequestedPublisher,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.inboundMessageRepository = inboundMessageRepository;
        this.extractedPayloadRepository = extractedPayloadRepository;
        this.userAccountRepository = userAccountRepository;
        this.animalRepository = animalRepository;
        this.lotRepository = lotRepository;
        this.planLimitPolicy = planLimitPolicy;
        this.billingAccessPolicy = billingAccessPolicy;
        this.outboundDeliveryRequestedPublisher = outboundDeliveryRequestedPublisher;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public void handleRequestedValidation(WhatsappPayloadValidationRequestedEvent event) {
        WhatsappInboundMessage inboundMessage = inboundMessageRepository.findByProviderMessageId(event.providerMessageId())
                .orElseThrow(() -> new IllegalStateException(
                        "Inbound message not found for provider message ID: " + event.providerMessageId()
                ));
        if (inboundMessage.status() == WhatsappProcessingStatus.AWAITING_CONFIRMATION
                || inboundMessage.status() == WhatsappProcessingStatus.REJECTED) {
            publishOutboundDelivery(inboundMessage, inboundMessage.status());
            return;
        }
        if (inboundMessage.status() == WhatsappProcessingStatus.FAILED) {
            return;
        }

        WhatsappExtractedPayload extractedPayload = extractedPayloadRepository.findByInboundMessageId(event.inboundMessageId())
                .orElseThrow(() -> new WhatsappWorkerRetryableException(
                        event.inboundMessageId(),
                        event.providerMessageId(),
                        "Extracted payload not yet visible for inbound message ID: " + event.inboundMessageId() + " — retrying after extraction transaction commit",
                        new IllegalStateException("Extracted payload not found")
                ));
        Map<String, Object> structuredPayload = parsePayload(extractedPayload.structuredPayload());
        String intent = extractedPayload.intent();

        Optional<UserAccount> maybeUser = resolveActiveUser(inboundMessage);
        if (maybeUser.isEmpty()) {
            reject(inboundMessage, "Validation rejected unauthorized phone number.");
            return;
        }

        UserAccount user = maybeUser.get();
        String requiredPermission = requiredPermission(intent);
        if (requiredPermission == null) {
            reject(inboundMessage, "Validation rejected unsupported intent: " + intent + ".");
            return;
        }
        if (!RolePermissionSummary.permissionsFor(user.role()).contains(requiredPermission)) {
            reject(inboundMessage, "Validation rejected permission denied for " + intent + ".");
            return;
        }

        try {
            billingAccessPolicy.requireWhatsappAllowed(user.farmId());
        } catch (BillingAccessDeniedException exception) {
            reject(inboundMessage, "Validation rejected billing status: " + exception.snapshot().status() + ".");
            return;
        }

        Optional<String> entityFailure = validateReferencedEntity(user.farmId(), structuredPayload, intent);
        if (entityFailure.isPresent()) {
            reject(inboundMessage, entityFailure.get());
            return;
        }

        WhatsappPlanLimitResult planLimit = planLimitPolicy.check(new WhatsappPlanLimitRequest(
                user.farmId(),
                user.id(),
                intent,
                OffsetDateTime.now(clock)
        ));
        if (!planLimit.permitted()) {
            reject(inboundMessage, "Validation rejected plan limit: " + planLimit.reason());
            return;
        }

        inboundMessageRepository.recordProcessingStep(
                inboundMessage.id(),
                WhatsappProcessingStatus.VALIDATED,
                "Payload validated for " + intent + ".",
                OffsetDateTime.now(clock)
        );
        WhatsappInboundMessage awaitingConfirmation = inboundMessageRepository.recordProcessingStep(
                inboundMessage.id(),
                WhatsappProcessingStatus.AWAITING_CONFIRMATION,
                "Validation passed; awaiting confirmation for " + intent + ".",
                OffsetDateTime.now(clock)
        );
        publishOutboundDelivery(awaitingConfirmation, WhatsappProcessingStatus.AWAITING_CONFIRMATION);
    }

    private Optional<UserAccount> resolveActiveUser(WhatsappInboundMessage inboundMessage) {
        Optional<UserAccount> maybeUser = inboundMessage.userId() == null
                ? userAccountRepository.findByPhone(inboundMessage.phoneNumber())
                : userAccountRepository.findById(inboundMessage.userId());
        return maybeUser
                .filter(UserAccount::active)
                .filter(user -> inboundMessage.farmId() != null)
                .filter(user -> user.farmId().equals(inboundMessage.farmId()));
    }

    private String requiredPermission(String intent) {
        return switch (intent) {
            case "register_weighing" -> "weighings.write";
            case "register_occurrence" -> "occurrences.write";
            case "register_feeding" -> "feeding.write";
            case "register_vaccine" -> "vaccines.write";
            case "register_medication" -> "medications.write";
            default -> null;
        };
    }

    private Optional<String> validateReferencedEntity(UUID farmId, Map<String, Object> payload, String intent) {
        String animalId = stringValue(payload, "animalId");
        if (!animalId.isBlank()) {
            try {
                if (animalRepository.findByIdAndFarmId(UUID.fromString(animalId), farmId).isPresent()) {
                    return Optional.empty();
                }
                return Optional.of("Validation rejected missing animal " + animalId + ".");
            } catch (IllegalArgumentException exception) {
                return Optional.of("Validation rejected missing animal " + animalId + ".");
            }
        }

        String animalIdentifier = stringValue(payload, "animalIdentifier");
        if (!animalIdentifier.isBlank()) {
            if (animalRepository.existsByFarmIdAndIdentifier(farmId, animalIdentifier)) {
                return Optional.empty();
            }
            return Optional.of("Validation rejected missing animal " + animalIdentifier + ".");
        }

        String lotId = stringValue(payload, "lotId");
        if (!lotId.isBlank()) {
            try {
                if (lotRepository.findByIdAndFarmId(UUID.fromString(lotId), farmId).isPresent()) {
                    return Optional.empty();
                }
                return Optional.of("Validation rejected missing lot " + lotId + ".");
            } catch (IllegalArgumentException exception) {
                return Optional.of("Validation rejected missing lot " + lotId + ".");
            }
        }

        if ("register_weighing".equals(intent)
                || "register_occurrence".equals(intent)
                || "register_vaccine".equals(intent)
                || "register_medication".equals(intent)) {
            return Optional.of("Validation rejected missing animal reference.");
        }
        return Optional.empty();
    }

    private String stringValue(Map<String, Object> payload, String fieldName) {
        Object value = payload.get(fieldName);
        if (value == null) {
            return "";
        }
        return value.toString().trim();
    }

    private Map<String, Object> parsePayload(String structuredPayload) {
        try {
            return objectMapper.readValue(structuredPayload, PAYLOAD_MAP);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Extracted payload is not valid JSON.", exception);
        }
    }

    private void reject(WhatsappInboundMessage inboundMessage, String details) {
        log.warn("Rejecting inbound message inboundMessageId={} providerMessageId={} reason={}",
                inboundMessage.id(), inboundMessage.providerMessageId(), details);
        WhatsappInboundMessage rejected = inboundMessageRepository.recordProcessingStep(
                inboundMessage.id(),
                WhatsappProcessingStatus.REJECTED,
                details,
                OffsetDateTime.now(clock)
        );
        publishOutboundDelivery(rejected, WhatsappProcessingStatus.REJECTED);
    }

    private void publishOutboundDelivery(WhatsappInboundMessage inboundMessage, WhatsappProcessingStatus status) {
        outboundDeliveryRequestedPublisher.publishRequested(new WhatsappOutboundDeliveryRequestedEvent(
                1,
                "whatsapp.outbound.delivery.requested.v1",
                UUID.randomUUID().toString(),
                inboundMessage.correlationId(),
                OffsetDateTime.now(clock),
                inboundMessage.id(),
                inboundMessage.providerMessageId(),
                status
        ));
    }
}
