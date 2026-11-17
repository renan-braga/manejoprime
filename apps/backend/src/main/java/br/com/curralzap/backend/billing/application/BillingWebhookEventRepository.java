package br.com.curralzap.backend.billing.application;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface BillingWebhookEventRepository {

    boolean existsByProviderAndEventId(String provider, String providerEventId);

    void insert(UUID id, String provider, String providerEventId, String eventType,
                String payload, OffsetDateTime createdAt);

    void markProcessed(String provider, String providerEventId, OffsetDateTime processedAt);
}
