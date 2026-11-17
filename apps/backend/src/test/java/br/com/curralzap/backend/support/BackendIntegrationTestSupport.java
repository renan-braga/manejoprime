package br.com.curralzap.backend.support;

import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=none"
})
public abstract class BackendIntegrationTestSupport {

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetDatabase() {
        clearStandardTables();
    }

    protected void clearStandardTables() {
        jdbcTemplate.update("delete from media_references");
        jdbcTemplate.update("delete from refresh_tokens");
        jdbcTemplate.update("delete from auth_sessions");
        jdbcTemplate.update("delete from whatsapp_transcripts");
        jdbcTemplate.update("delete from whatsapp_extracted_payloads");
        jdbcTemplate.update("delete from whatsapp_outbound_messages");
        jdbcTemplate.update("delete from whatsapp_processing_steps");
        jdbcTemplate.update("delete from whatsapp_inbound_messages");
        jdbcTemplate.update("delete from alimentacoes");
        jdbcTemplate.update("delete from medications");
        jdbcTemplate.update("delete from vaccines");
        jdbcTemplate.update("delete from occurrences");
        jdbcTemplate.update("delete from weighings");
        jdbcTemplate.update("delete from offer_animals");
        jdbcTemplate.update("delete from offers");
        jdbcTemplate.update("delete from animals");
        jdbcTemplate.update("delete from billing_webhook_events");
        jdbcTemplate.update("delete from billing_subscriptions");
        jdbcTemplate.update("delete from user_farm_memberships");
        jdbcTemplate.update("update farms set owner_user_id = null where owner_user_id is not null");
        jdbcTemplate.update("delete from users");
        jdbcTemplate.update("delete from lots");
        jdbcTemplate.update("delete from farms");
    }

    protected OffsetDateTime nowUtc() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    protected UUID insertFarm(String name) {
        UUID farmId = UUID.randomUUID();
        OffsetDateTime now = nowUtc();

        jdbcTemplate.update(
                """
                        insert into farms (id, nome, rendimento_carcaca, created_at, updated_at)
                        values (?, ?, ?, ?, ?)
                        """,
                farmId,
                name,
                50.00,
                Timestamp.from(now.toInstant()),
                Timestamp.from(now.toInstant())
        );

        return farmId;
    }

    protected UserAccount insertUser(UUID farmId, String phone, String name, UserRole role, boolean active) {
        return insertUser(farmId, phone, name, role.name().toLowerCase(), active);
    }

    protected UserAccount insertUser(UUID farmId, String phone, String name, String role, boolean active) {
        UUID userId = UUID.randomUUID();
        OffsetDateTime now = nowUtc();

        jdbcTemplate.update(
                """
                        insert into users (id, farm_id, telefone, nome, role, ativo, created_at, updated_at)
                        values (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                userId,
                farmId,
                normalizePhone(phone),
                name,
                role,
                active,
                Timestamp.from(now.toInstant()),
                Timestamp.from(now.toInstant())
        );

        return new UserAccount(
                userId,
                farmId,
                normalizePhone(phone),
                null,
                name,
                UserRole.valueOf(role.toUpperCase()),
                active,
                now,
                now
        );
    }

    protected UUID insertLot(UUID farmId, String name, String purpose) {
        return insertLot(farmId, name, purpose, null);
    }

    protected UUID insertLot(UUID farmId, String name, String purpose, String pastureArea) {
        UUID lotId = UUID.randomUUID();
        OffsetDateTime now = nowUtc();

        jdbcTemplate.update(
                """
                        insert into lots (id, farm_id, nome, finalidade, pasto_area, created_at, updated_at)
                        values (?, ?, ?, ?, ?, ?, ?)
                        """,
                lotId,
                farmId,
                name,
                purpose,
                pastureArea,
                Timestamp.from(now.toInstant()),
                Timestamp.from(now.toInstant())
        );

        return lotId;
    }

    protected UUID insertAnimal(UUID farmId, UUID lotId, String identifier) {
        return insertAnimal(farmId, lotId, identifier, null, null, "macho", "ativo", null, null, null);
    }

    protected UUID insertAnimal(
            UUID farmId,
            UUID lotId,
            String identifier,
            String name,
            String breed,
            String sex,
            String status,
            LocalDate birthDate,
            LocalDate entryDate,
            BigDecimal birthWeightKg
    ) {
        UUID animalId = UUID.randomUUID();
        OffsetDateTime now = nowUtc();

        jdbcTemplate.update(
                """
                        insert into animals (
                            id, farm_id, lot_id, identificacao, nome, raca, sexo, data_nascimento,
                            peso_nascimento, status, data_entrada, created_at, updated_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                animalId,
                farmId,
                lotId,
                identifier,
                name,
                breed,
                sex,
                birthDate != null ? Date.valueOf(birthDate) : null,
                birthWeightKg,
                status,
                entryDate != null ? Date.valueOf(entryDate) : null,
                Timestamp.from(now.toInstant()),
                Timestamp.from(now.toInstant())
        );

        return animalId;
    }

    protected UUID insertOccurrenceRecord(
            UUID farmId,
            UUID animalId,
            UUID recordedBy,
            String status,
            UUID resolvedBy
    ) {
        UUID occurrenceId = UUID.randomUUID();
        OffsetDateTime now = nowUtc();

        jdbcTemplate.update(
                """
                        insert into occurrences (
                            id, farm_id, animal_id, tipo, gravidade, descricao, foto_url, origem,
                            registrado_por, status, resolvido_por, created_at, updated_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                occurrenceId,
                farmId,
                animalId,
                "health",
                "high",
                "Animal limping.",
                null,
                "app",
                recordedBy,
                status,
                resolvedBy,
                Timestamp.from(now.toInstant()),
                Timestamp.from(now.toInstant())
        );

        return occurrenceId;
    }

    protected UUID insertWhatsappInboundMessage(
            String providerMessageId,
            String phoneNumber,
            UUID farmId,
            UUID userId,
            String correlationId,
            String eventType,
            String messageType,
            String status,
            String failureReason
    ) {
        UUID inboundMessageId = UUID.randomUUID();
        OffsetDateTime now = nowUtc();

        jdbcTemplate.update(
                """
                        insert into whatsapp_inbound_messages (
                            id, provider_message_id, event_type, phone_number, farm_id, user_id,
                            correlation_id, message_type, raw_payload, status, failure_reason, received_at, created_at, updated_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                inboundMessageId,
                providerMessageId,
                eventType,
                normalizePhone(phoneNumber),
                farmId,
                userId,
                correlationId,
                messageType,
                "{\"event\":\"messages.upsert\"}",
                status,
                failureReason,
                Timestamp.from(now.toInstant()),
                Timestamp.from(now.toInstant()),
                Timestamp.from(now.toInstant())
        );

        return inboundMessageId;
    }

    protected UUID insertAuthSession(
            UUID farmId,
            UUID userId,
            OffsetDateTime revokedAt,
            String revocationReason
    ) {
        UUID sessionId = UUID.randomUUID();
        OffsetDateTime now = nowUtc();

        jdbcTemplate.update(
                """
                        insert into auth_sessions (
                            id, farm_id, user_id, revoked_at, revocation_reason, created_at, updated_at
                        ) values (?, ?, ?, ?, ?, ?, ?)
                        """,
                sessionId,
                farmId,
                userId,
                revokedAt != null ? Timestamp.from(revokedAt.toInstant()) : null,
                revocationReason,
                Timestamp.from(now.toInstant()),
                Timestamp.from(now.toInstant())
        );

        return sessionId;
    }

    protected UUID insertRefreshToken(
            UUID sessionId,
            UUID farmId,
            UUID userId,
            OffsetDateTime expiresAt,
            OffsetDateTime consumedAt,
            OffsetDateTime revokedAt,
            String revocationReason,
            UUID replacedByTokenId
    ) {
        UUID tokenId = UUID.randomUUID();
        OffsetDateTime now = nowUtc();

        jdbcTemplate.update(
                """
                        insert into refresh_tokens (
                            id, session_id, farm_id, user_id, expires_at, consumed_at, revoked_at,
                            revocation_reason, replaced_by_token_id, created_at, updated_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                tokenId,
                sessionId,
                farmId,
                userId,
                Timestamp.from(expiresAt.toInstant()),
                consumedAt != null ? Timestamp.from(consumedAt.toInstant()) : null,
                revokedAt != null ? Timestamp.from(revokedAt.toInstant()) : null,
                revocationReason,
                replacedByTokenId,
                Timestamp.from(now.toInstant()),
                Timestamp.from(now.toInstant())
        );

        return tokenId;
    }

    protected void insertFarmMembership(UUID userId, UUID farmId, String role) {
        OffsetDateTime now = nowUtc();
        jdbcTemplate.update(
                "insert into user_farm_memberships (user_id, farm_id, role, joined_at) values (?, ?, ?, ?)",
                userId, farmId, role, Timestamp.from(now.toInstant())
        );
    }

    protected void setFarmOwner(UUID farmId, UUID ownerUserId) {
        jdbcTemplate.update("update farms set owner_user_id = ? where id = ?", ownerUserId, farmId);
    }

    protected UUID insertBillingSubscription(
            UUID farmId,
            String plan,
            String status,
            OffsetDateTime graceUntil,
            boolean billingExempt
    ) {
        UUID subscriptionId = UUID.randomUUID();
        OffsetDateTime now = nowUtc();

        jdbcTemplate.update(
                """
                        insert into billing_subscriptions (
                            id, farm_id, provider, provider_customer_id, provider_subscription_id,
                            plan, status, current_period_end, grace_until, suspended_at, canceled_at,
                            billing_exempt, exemption_reason, created_at, updated_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                subscriptionId,
                farmId,
                "manual",
                null,
                null,
                plan,
                status,
                null,
                graceUntil != null ? Timestamp.from(graceUntil.toInstant()) : null,
                "suspended".equals(status) ? Timestamp.from(now.toInstant()) : null,
                "canceled".equals(status) ? Timestamp.from(now.toInstant()) : null,
                billingExempt,
                billingExempt ? "test access" : null,
                Timestamp.from(now.toInstant()),
                Timestamp.from(now.toInstant())
        );

        return subscriptionId;
    }

    protected UUID insertWeighing(
            UUID farmId,
            UUID animalId,
            UUID registradoPor,
            BigDecimal pesoKg,
            LocalDate dataWeighing
    ) {
        UUID weighingId = UUID.randomUUID();
        OffsetDateTime now = nowUtc();

        jdbcTemplate.update(
                """
                        insert into weighings (
                            id, farm_id, animal_id, peso_kg, data_weighing, tipo, origem, registrado_por, created_at, updated_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                weighingId,
                farmId,
                animalId,
                pesoKg,
                Date.valueOf(dataWeighing),
                "rotina",
                "app",
                registradoPor,
                Timestamp.from(now.toInstant()),
                Timestamp.from(now.toInstant())
        );

        return weighingId;
    }

    protected String normalizePhone(String phone) {
        return phone.replaceFirst("^\\+", "").replaceAll("[^\\d]", "");
    }
}
