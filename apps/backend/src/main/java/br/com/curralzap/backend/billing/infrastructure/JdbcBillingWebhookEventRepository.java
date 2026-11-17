package br.com.curralzap.backend.billing.infrastructure;

import br.com.curralzap.backend.billing.application.BillingWebhookEventRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
class JdbcBillingWebhookEventRepository implements BillingWebhookEventRepository {

    private final JdbcTemplate jdbcTemplate;

    JdbcBillingWebhookEventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean existsByProviderAndEventId(String provider, String providerEventId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from billing_webhook_events where provider = ? and provider_event_id = ?",
                Integer.class, provider, providerEventId);
        return count != null && count > 0;
    }

    @Override
    public void insert(UUID id, String provider, String providerEventId, String eventType,
                       String payload, OffsetDateTime createdAt) {
        jdbcTemplate.update(
                """
                insert into billing_webhook_events (id, provider, provider_event_id, event_type, payload, created_at)
                values (?, ?, ?, ?, ?, ?)
                """,
                id, provider, providerEventId, eventType, payload,
                Timestamp.from(createdAt.toInstant()));
    }

    @Override
    public void markProcessed(String provider, String providerEventId, OffsetDateTime processedAt) {
        jdbcTemplate.update(
                "update billing_webhook_events set processed_at = ? where provider = ? and provider_event_id = ?",
                Timestamp.from(processedAt.toInstant()), provider, providerEventId);
    }
}
