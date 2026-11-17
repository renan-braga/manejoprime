package br.com.curralzap.backend.billing.infrastructure;

import br.com.curralzap.backend.billing.application.BillingSubscriptionRepository;
import br.com.curralzap.backend.billing.domain.BillingSubscription;
import br.com.curralzap.backend.billing.domain.BillingSubscriptionStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

@Repository
class JdbcBillingSubscriptionRepository implements BillingSubscriptionRepository {

    private static final String SELECT_ALL =
            "select id, farm_id, provider, provider_customer_id, provider_subscription_id," +
            " plan, status, current_period_end, grace_until, suspended_at, canceled_at," +
            " billing_exempt, exemption_reason from billing_subscriptions ";

    private final JdbcTemplate jdbcTemplate;

    JdbcBillingSubscriptionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<BillingSubscription> findByFarmId(UUID farmId) {
        return jdbcTemplate.query(
                SELECT_ALL + "where farm_id = ?",
                (rs, n) -> map(rs),
                farmId
        ).stream().findFirst();
    }

    @Override
    public Optional<BillingSubscription> findByAccountOwnerFarmId(UUID farmId) {
        return jdbcTemplate.query(
                "select bs.id, bs.farm_id, bs.provider, bs.provider_customer_id, bs.provider_subscription_id," +
                " bs.plan, bs.status, bs.current_period_end, bs.grace_until, bs.suspended_at, bs.canceled_at," +
                " bs.billing_exempt, bs.exemption_reason" +
                " from billing_subscriptions bs" +
                " join farms f on f.id = bs.farm_id" +
                " where f.owner_user_id = (select owner_user_id from farms where id = ?)" +
                " and f.owner_user_id is not null",
                (rs, n) -> map(rs),
                farmId
        ).stream().findFirst();
    }

    @Override
    public Optional<OffsetDateTime> findFarmCreatedAt(UUID farmId) {
        return jdbcTemplate.query(
                "select created_at from farms where id = ?",
                (rs, n) -> dt(rs.getTimestamp("created_at")),
                farmId
        ).stream().findFirst();
    }

    @Override
    public Optional<BillingSubscription> findByProviderAndSubscriptionId(String provider, String subscriptionId) {
        return jdbcTemplate.query(
                SELECT_ALL + "where provider = ? and provider_subscription_id = ?",
                (rs, n) -> map(rs),
                provider,
                subscriptionId
        ).stream().findFirst();
    }

    @Override
    public void updateStatus(
            UUID id,
            BillingSubscriptionStatus status,
            OffsetDateTime currentPeriodEnd,
            OffsetDateTime graceUntil,
            OffsetDateTime suspendedAt,
            OffsetDateTime canceledAt,
            OffsetDateTime updatedAt
    ) {
        jdbcTemplate.update(
                "update billing_subscriptions" +
                " set status = ?, current_period_end = ?, grace_until = ?," +
                "     suspended_at = ?, canceled_at = ?, updated_at = ?" +
                " where id = ?",
                status.databaseValue(),
                ts(currentPeriodEnd),
                ts(graceUntil),
                ts(suspendedAt),
                ts(canceledAt),
                ts(updatedAt),
                id
        );
    }

    @Override
    public void upsertForCheckout(
            UUID farmId,
            String plan,
            String provider,
            String providerCustomerId,
            String providerSubscriptionId,
            OffsetDateTime now
    ) {
        int updated = jdbcTemplate.update(
                "update billing_subscriptions" +
                " set provider = ?, provider_customer_id = ?, provider_subscription_id = ?," +
                "     plan = ?, status = 'payment_required', updated_at = ?" +
                " where farm_id = ?",
                provider, providerCustomerId, providerSubscriptionId, plan, ts(now), farmId
        );
        if (updated == 0) {
            jdbcTemplate.update(
                    "insert into billing_subscriptions" +
                    " (id, farm_id, provider, provider_customer_id, provider_subscription_id," +
                    "  plan, status, billing_exempt, created_at, updated_at)" +
                    " values (?, ?, ?, ?, ?, ?, 'payment_required', false, ?, ?)",
                    UUID.randomUUID(), farmId, provider, providerCustomerId, providerSubscriptionId,
                    plan, ts(now), ts(now)
            );
        }
    }

    @Override
    public void activateDirectCharge(UUID farmId, OffsetDateTime currentPeriodEnd, OffsetDateTime now) {
        jdbcTemplate.update(
                "update billing_subscriptions" +
                " set status = 'active', current_period_end = ?, updated_at = ?" +
                " where farm_id = ?",
                ts(currentPeriodEnd), ts(now), farmId
        );
    }

    private static BillingSubscription map(ResultSet rs) throws SQLException {
        return new BillingSubscription(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("farm_id")),
                rs.getString("provider"),
                rs.getString("provider_customer_id"),
                rs.getString("provider_subscription_id"),
                rs.getString("plan"),
                BillingSubscriptionStatus.fromDatabaseValue(rs.getString("status")),
                dt(rs.getTimestamp("current_period_end")),
                dt(rs.getTimestamp("grace_until")),
                dt(rs.getTimestamp("suspended_at")),
                dt(rs.getTimestamp("canceled_at")),
                rs.getBoolean("billing_exempt"),
                rs.getString("exemption_reason")
        );
    }

    private static OffsetDateTime dt(Timestamp t) {
        return t == null ? null : OffsetDateTime.ofInstant(t.toInstant(), ZoneOffset.UTC);
    }

    private static Timestamp ts(OffsetDateTime dt) {
        return dt == null ? null : Timestamp.from(dt.toInstant());
    }
}
