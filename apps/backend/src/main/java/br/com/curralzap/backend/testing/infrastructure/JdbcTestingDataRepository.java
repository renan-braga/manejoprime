package br.com.curralzap.backend.testing.infrastructure;

import br.com.curralzap.backend.testing.application.TestingDataRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
class JdbcTestingDataRepository implements TestingDataRepository {

    private final JdbcTemplate jdbc;

    JdbcTestingDataRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void insertActiveSubscription(UUID farmId, OffsetDateTime now) {
        OffsetDateTime periodEnd = now.plusYears(10);
        jdbc.update(
                "insert into billing_subscriptions" +
                " (id, farm_id, plan, status, current_period_end, billing_exempt, created_at, updated_at)" +
                " values (?, ?, 'business', 'active', ?, true, ?, ?)" +
                " on conflict (farm_id) do update" +
                " set status = 'active', current_period_end = excluded.current_period_end," +
                "     billing_exempt = true, updated_at = excluded.updated_at",
                UUID.randomUUID(), farmId, ts(periodEnd), ts(now), ts(now)
        );
    }

    @Override
    public void deleteAllForFarm(UUID farmId) {
        // 1. Break circular reference (farms.owner_user_id → users).
        // Also covers secondary farms created during test (e.g., "create second farm" test),
        // whose owner_user_id references the same test user.
        jdbc.update("update farms set owner_user_id = null where id = ?", farmId);
        jdbc.update(
                "update farms set owner_user_id = null" +
                " where owner_user_id in (select id from users where farm_id = ?)",
                farmId
        );

        // 2. WhatsApp pipeline records (farm_id messages + null-farm multi-selection messages)
        jdbc.update(
                "delete from whatsapp_processing_steps where inbound_message_id in" +
                " (select id from whatsapp_inbound_messages" +
                "  where farm_id = ? or (farm_id is null and user_id in (select id from users where farm_id = ?)))",
                farmId, farmId
        );
        jdbc.update(
                "delete from whatsapp_transcripts where inbound_message_id in" +
                " (select id from whatsapp_inbound_messages" +
                "  where farm_id = ? or (farm_id is null and user_id in (select id from users where farm_id = ?)))",
                farmId, farmId
        );
        jdbc.update(
                "delete from whatsapp_extracted_payloads where inbound_message_id in" +
                " (select id from whatsapp_inbound_messages" +
                "  where farm_id = ? or (farm_id is null and user_id in (select id from users where farm_id = ?)))",
                farmId, farmId
        );
        jdbc.update(
                "delete from whatsapp_audio_media where inbound_message_id in" +
                " (select id from whatsapp_inbound_messages" +
                "  where farm_id = ? or (farm_id is null and user_id in (select id from users where farm_id = ?)))",
                farmId, farmId
        );
        jdbc.update("delete from whatsapp_outbound_messages where farm_id = ?", farmId);
        jdbc.update(
                "delete from whatsapp_inbound_messages" +
                " where farm_id = ? or (farm_id is null and user_id in (select id from users where farm_id = ?))",
                farmId, farmId
        );
        jdbc.update(
                "delete from whatsapp_farm_sessions" +
                " where phone_number in (select telefone from users where farm_id = ?)",
                farmId
        );

        // 3. Domain records
        jdbc.update("delete from weighings where farm_id = ?", farmId);
        jdbc.update("delete from medications where farm_id = ?", farmId);
        jdbc.update("delete from vaccines where farm_id = ?", farmId);
        jdbc.update("delete from occurrences where farm_id = ?", farmId);
        jdbc.update("delete from alimentacoes where farm_id = ?", farmId);
        jdbc.update(
                "delete from offer_animals" +
                " where offer_id in (select id from offers where farm_id = ?)",
                farmId
        );
        jdbc.update("delete from offers where farm_id = ?", farmId);
        jdbc.update("delete from animals where farm_id = ?", farmId);
        jdbc.update("delete from lots where farm_id = ?", farmId);

        // 4. Billing
        jdbc.update("delete from billing_customer_data where farm_id = ?", farmId);
        jdbc.update("delete from billing_subscriptions where farm_id = ?", farmId);

        // 5. Auth and users
        jdbc.update("delete from invitations where farm_id = ?", farmId);
        // Delete memberships across ALL farms for test users (secondary farms created in test)
        jdbc.update(
                "delete from user_farm_memberships" +
                " where user_id in (select id from users where farm_id = ?)",
                farmId
        );
        // Delete auth records explicitly — V3 fk_auth_sessions_user_id has no cascade
        jdbc.update(
                "delete from refresh_tokens" +
                " where user_id in (select id from users where farm_id = ?)",
                farmId
        );
        jdbc.update(
                "delete from auth_sessions" +
                " where user_id in (select id from users where farm_id = ?)",
                farmId
        );
        jdbc.update("delete from users where farm_id = ?", farmId);

        // 6. Farm itself
        jdbc.update("delete from farms where id = ?", farmId);
    }

    private static Timestamp ts(OffsetDateTime dt) {
        return dt == null ? null : Timestamp.from(dt.toInstant());
    }
}
