package br.com.curralzap.backend.billing.application;

import br.com.curralzap.backend.billing.domain.BillingSubscription;
import br.com.curralzap.backend.billing.domain.BillingSubscriptionStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.UUID;

@Service
public class AsaasWebhookProcessor {

    private static final Logger log = LoggerFactory.getLogger(AsaasWebhookProcessor.class);
    private static final String PROVIDER = "asaas";
    private static final int GRACE_PERIOD_DAYS = 7;

    private final ObjectMapper objectMapper;
    private final BillingWebhookEventRepository webhookEventRepository;
    private final BillingSubscriptionRepository subscriptionRepository;

    public AsaasWebhookProcessor(
            ObjectMapper objectMapper,
            BillingWebhookEventRepository webhookEventRepository,
            BillingSubscriptionRepository subscriptionRepository
    ) {
        this.objectMapper = objectMapper;
        this.webhookEventRepository = webhookEventRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Transactional
    public void process(String rawPayload) {
        JsonNode root;
        try {
            root = objectMapper.readTree(rawPayload);
        } catch (JsonProcessingException e) {
            log.warn("Asaas webhook: could not parse payload — {}", e.getMessage());
            return;
        }

        String event = root.path("event").asText(null);
        if (event == null) {
            log.warn("Asaas webhook: missing event field");
            return;
        }

        String entityId = extractEntityId(root, event);
        if (entityId == null) {
            log.warn("Asaas webhook: could not extract entity id for event={}", event);
            return;
        }

        String idempotencyKey = event + ":" + entityId;

        if (webhookEventRepository.existsByProviderAndEventId(PROVIDER, idempotencyKey)) {
            log.debug("Asaas webhook: duplicate event skipped key={}", idempotencyKey);
            return;
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        webhookEventRepository.insert(UUID.randomUUID(), PROVIDER, idempotencyKey, event, rawPayload, now);

        String subscriptionId = extractSubscriptionId(root, event);
        if (subscriptionId == null) {
            log.info("Asaas webhook: no subscription reference in event={}, skipping status update", event);
            webhookEventRepository.markProcessed(PROVIDER, idempotencyKey, now);
            return;
        }

        Optional<BillingSubscription> found = subscriptionRepository.findByProviderAndSubscriptionId(PROVIDER, subscriptionId);
        if (found.isEmpty()) {
            log.info("Asaas webhook: subscription not found providerSubscriptionId={} event={}", subscriptionId, event);
            webhookEventRepository.markProcessed(PROVIDER, idempotencyKey, now);
            return;
        }

        BillingSubscription subscription = found.get();
        applyTransition(subscription, event, root, now);
        webhookEventRepository.markProcessed(PROVIDER, idempotencyKey, now);
    }

    private void applyTransition(BillingSubscription sub, String event, JsonNode root, OffsetDateTime now) {
        switch (event) {
            case "PAYMENT_RECEIVED", "PAYMENT_CONFIRMED", "PAYMENT_RESTORED" -> {
                OffsetDateTime periodEnd = parsePeriodEnd(root.path("payment").path("dueDate").asText(null), now);
                subscriptionRepository.updateStatus(sub.id(), BillingSubscriptionStatus.ACTIVE,
                        periodEnd, null, null, null, now);
                log.info("Asaas: farm={} → ACTIVE ({})", sub.farmId(), event);
            }
            case "PAYMENT_OVERDUE" -> {
                OffsetDateTime graceUntil = now.plusDays(GRACE_PERIOD_DAYS);
                subscriptionRepository.updateStatus(sub.id(), BillingSubscriptionStatus.PAST_DUE,
                        sub.currentPeriodEnd(), graceUntil, null, null, now);
                log.info("Asaas: farm={} → PAST_DUE graceUntil={}", sub.farmId(), graceUntil);
            }
            case "SUBSCRIPTION_INACTIVATED", "SUBSCRIPTION_CANCELED" -> {
                subscriptionRepository.updateStatus(sub.id(), BillingSubscriptionStatus.SUSPENDED,
                        sub.currentPeriodEnd(), null, now, null, now);
                log.info("Asaas: farm={} → SUSPENDED ({})", sub.farmId(), event);
            }
            case "SUBSCRIPTION_DELETED" -> {
                subscriptionRepository.updateStatus(sub.id(), BillingSubscriptionStatus.CANCELED,
                        sub.currentPeriodEnd(), null, sub.suspendedAt(), now, now);
                log.info("Asaas: farm={} → CANCELED", sub.farmId());
            }
            case "SUBSCRIPTION_ACTIVATED" -> {
                subscriptionRepository.updateStatus(sub.id(), BillingSubscriptionStatus.ACTIVE,
                        sub.currentPeriodEnd(), null, null, null, now);
                log.info("Asaas: farm={} → ACTIVE (SUBSCRIPTION_ACTIVATED)", sub.farmId());
            }
            default -> log.debug("Asaas webhook: unhandled event={}", event);
        }
    }

    private static String extractEntityId(JsonNode root, String event) {
        if (event.startsWith("PAYMENT_")) {
            String id = root.path("payment").path("id").asText(null);
            return id != null ? id : root.path("subscription").path("id").asText(null);
        }
        return root.path("subscription").path("id").asText(null);
    }

    private static String extractSubscriptionId(JsonNode root, String event) {
        if (event.startsWith("PAYMENT_")) {
            return root.path("payment").path("subscription").asText(null);
        }
        return root.path("subscription").path("id").asText(null);
    }

    private static OffsetDateTime parsePeriodEnd(String dueDate, OffsetDateTime fallback) {
        if (dueDate == null || dueDate.isBlank()) return fallback.plusDays(33);
        try {
            return LocalDate.parse(dueDate).plusDays(33).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
        } catch (DateTimeParseException e) {
            return fallback.plusDays(33);
        }
    }
}
