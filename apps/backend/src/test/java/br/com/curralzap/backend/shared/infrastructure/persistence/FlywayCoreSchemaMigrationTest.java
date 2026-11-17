package br.com.curralzap.backend.shared.infrastructure.persistence;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class FlywayCoreSchemaMigrationTest {

    @Test
    void migrationsBootSuccessfullyOnCleanDatabase() {
        TestDatabase database = migratedDatabase("boot");

        Integer appliedMigrations = database.jdbcTemplate().queryForObject(
                "select count(*) from flyway_schema_history where success = true and version in ('1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11', '12', '13', '14', '15', '16', '17', '18', '19', '20', '21')",
                Integer.class
        );

        assertThat(appliedMigrations).isEqualTo(21);
        assertThat(database.tableNames()).contains(
                "alimentacoes",
                "animals",
                "auth_sessions",
                "farms",
                "invitations",
                "lots",
                "media_references",
                "medications",
                "occurrences",
                "vaccines",
                "whatsapp_audio_media",
                "whatsapp_extracted_payloads",
                "whatsapp_inbound_messages",
                "whatsapp_outbound_messages",
                "whatsapp_processing_steps",
                "whatsapp_transcripts",
                "weighings",
                "refresh_tokens",
                "users"
        );
        assertThat(database.tableNames()).doesNotContain("magic_link_tokens", "sms_otp_codes");
    }

    @Test
    void uniqueConstraintsAreEnforcedForCoreIdentityLookups() {
        TestDatabase database = migratedDatabase("unique");
        UUID farmA = database.insertFarm("Fazenda A");
        UUID farmB = database.insertFarm("Fazenda B");

        database.insertUser(farmA, "5511999990001", "Admin A", "admin", true);

        assertThatThrownBy(() -> database.insertUser(farmB, "5511999990001", "Admin B", "worker", true))
                .isInstanceOf(DataIntegrityViolationException.class);

        database.insertAnimal(farmA, null, "BR-001", "macho", "ativo");
        database.insertAnimal(farmB, null, "BR-001", "macho", "ativo");

        assertThatThrownBy(() -> database.insertAnimal(farmA, null, "BR-001", "femea", "ativo"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void foreignKeysAndCheckConstraintsAreEnforced() {
        TestDatabase database = migratedDatabase("constraints");
        UUID farmId = database.insertFarm("Fazenda Sede");
        UUID lotId = database.insertLot(farmId, "Lote 1", "raising");

        assertThatThrownBy(() -> database.insertLot(UUID.randomUUID(), "Lote invalido", "raising"))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> database.insertUser(farmId, "5511999990002", "Invalido", "owner", true))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> database.insertAnimal(farmId, lotId, "BR-010", "desconhecido", "ativo"))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> database.insertAnimal(farmId, UUID.randomUUID(), "BR-011", "macho", "ativo"))
                .isInstanceOf(DataIntegrityViolationException.class);

        UUID userId = database.insertUser(farmId, "5511999990003", "Auth User", "worker", true);
        UUID animalId = database.insertAnimal(farmId, lotId, "BR-012", "macho", "ativo");
        UUID occurrenceId = database.insertOccurrence(farmId, animalId, userId);

        UUID occurrencePhotoReferenceId = database.insertMediaReference(
                "occurrence",
                occurrenceId,
                "occurrence_photo",
                "https://media.example/occurrences/photo.jpg",
                null,
                "image/jpeg",
                null,
                null
        );

        assertThat(occurrencePhotoReferenceId).isNotNull();

        UUID sessionId = database.insertAuthSession(farmId, userId, null, null);

        assertThatThrownBy(() -> database.insertAuthSession(farmId, userId, OffsetDateTime.now(ZoneOffset.UTC), null))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> database.insertRefreshToken(UUID.randomUUID(), farmId, userId, null, null, null, null))
                .isInstanceOf(DataIntegrityViolationException.class);

        database.insertRefreshToken(sessionId, farmId, userId, null, null, null, null);

        assertThatThrownBy(() -> database.insertRefreshToken(
                sessionId,
                farmId,
                userId,
                null,
                OffsetDateTime.now(ZoneOffset.UTC),
                "unsupported_reason",
                null
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> database.insertRefreshToken(
                sessionId,
                farmId,
                userId,
                null,
                null,
                null,
                UUID.randomUUID()
        )).isInstanceOf(DataIntegrityViolationException.class);

        database.insertWhatsappInboundMessage("wamid.valid", "5511999990003", "messages.upsert", "conversation", "RECEIVED");

        assertThatThrownBy(() -> database.insertWhatsappInboundMessage(
                "wamid.valid",
                "5511999990003",
                "messages.upsert",
                "conversation",
                "RECEIVED"
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> database.insertWhatsappInboundMessage(
                "wamid.invalid-status",
                "5511999990003",
                "messages.upsert",
                "conversation",
                "UNKNOWN"
        )).isInstanceOf(DataIntegrityViolationException.class);

        UUID inboundMessageId = database.insertWhatsappInboundMessage(
                "wamid.with-steps",
                "5511999990003",
                null,
                null,
                "corr-wa-flyway-1",
                "messages.upsert",
                "audioMessage",
                "RECEIVED",
                null
        );

        database.insertWhatsappProcessingStep(
                inboundMessageId,
                "MEDIA_DOWNLOADED",
                "Media downloaded successfully."
        );
        database.insertWhatsappProcessingStep(
                inboundMessageId,
                "TRANSCODED",
                "Audio transcoded successfully."
        );

        assertThatThrownBy(() -> database.insertWhatsappProcessingStep(
                inboundMessageId,
                "UNKNOWN",
                "Invalid step status."
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> database.insertWhatsappProcessingStep(
                UUID.randomUUID(),
                "MEDIA_DOWNLOADED",
                "Missing inbound message."
        )).isInstanceOf(DataIntegrityViolationException.class);

        UUID audioMediaId = database.insertWhatsappAudioMedia(
                inboundMessageId,
                "https://media.example/audio.ogg",
                "audio/ogg",
                "whatsapp/audio/audio.ogg",
                "audio/mpeg",
                "whatsapp/audio/audio-transcoded.mp3",
                OffsetDateTime.now(ZoneOffset.UTC)
        );

        assertThat(audioMediaId).isNotNull();

        UUID whatsappSourceReferenceId = database.insertMediaReference(
                "whatsapp_audio_media",
                audioMediaId,
                "whatsapp_source_audio",
                "https://media.example/audio.ogg",
                null,
                "audio/ogg",
                null,
                null
        );
        UUID whatsappStoredReferenceId = database.insertMediaReference(
                "whatsapp_audio_media",
                audioMediaId,
                "whatsapp_downloaded_audio",
                null,
                "whatsapp/audio/audio.ogg",
                "audio/ogg",
                2048L,
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        );

        assertThat(whatsappSourceReferenceId).isNotNull();
        assertThat(whatsappStoredReferenceId).isNotNull();

        assertThatThrownBy(() -> database.insertMediaReference(
                "occurrence",
                occurrenceId,
                "occurrence_photo",
                "https://media.example/occurrences/duplicate.jpg",
                null,
                "image/jpeg",
                null,
                null
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> database.insertMediaReference(
                "unknown_owner",
                occurrenceId,
                "occurrence_photo",
                "https://media.example/occurrences/invalid-owner.jpg",
                null,
                "image/jpeg",
                null,
                null
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> database.insertMediaReference(
                "occurrence",
                occurrenceId,
                "unknown_purpose",
                "https://media.example/occurrences/invalid-purpose.jpg",
                null,
                "image/jpeg",
                null,
                null
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> database.insertMediaReference(
                "occurrence",
                occurrenceId,
                "occurrence_photo",
                "https://media.example/occurrences/two-locations.jpg",
                "occurrences/photo.jpg",
                "image/jpeg",
                null,
                null
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> database.insertMediaReference(
                "occurrence",
                occurrenceId,
                "occurrence_photo",
                null,
                null,
                "image/jpeg",
                null,
                null
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> database.insertMediaReference(
                "occurrence",
                occurrenceId,
                "occurrence_photo",
                "ftp://media.example/occurrences/invalid-scheme.jpg",
                null,
                "image/jpeg",
                null,
                null
        )).isInstanceOf(DataIntegrityViolationException.class);

        UUID transcriptId = database.insertWhatsappTranscript(
                inboundMessageId,
                "openai",
                "gpt-4o-mini-transcribe",
                "Registrar peso de 18 arrobas."
        );

        assertThat(transcriptId).isNotNull();

        UUID extractedPayloadId = database.insertWhatsappExtractedPayload(
                inboundMessageId,
                "openai",
                "gpt-5.4-mini",
                "register_weighing",
                "{\"animalId\":\"A-1\",\"weightKg\":324.5}"
        );

        assertThat(extractedPayloadId).isNotNull();

        UUID outboundMessageId = database.insertWhatsappOutboundMessage(
                inboundMessageId,
                farmId,
                userId,
                "corr-wa-flyway-1",
                "5511999990003",
                "text",
                "{\"text\":\"Confirmado\"}",
                null,
                "QUEUED",
                null
        );

        assertThat(outboundMessageId).isNotNull();

        assertThatThrownBy(() -> database.insertWhatsappAudioMedia(
                inboundMessageId,
                "https://media.example/audio-duplicate.ogg",
                "audio/ogg",
                "whatsapp/audio/audio-duplicate.ogg",
                null,
                null,
                null
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> database.insertWhatsappAudioMedia(
                UUID.randomUUID(),
                "https://media.example/audio-missing.ogg",
                "audio/ogg",
                "whatsapp/audio/audio-missing.ogg",
                null,
                null,
                null
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> database.insertWhatsappTranscript(
                inboundMessageId,
                "openai",
                "gpt-4o-mini-transcribe",
                "Duplicate transcript."
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> database.insertWhatsappTranscript(
                UUID.randomUUID(),
                "openai",
                "gpt-4o-mini-transcribe",
                "Missing inbound message."
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> database.insertWhatsappExtractedPayload(
                inboundMessageId,
                "openai",
                "gpt-5.4-mini",
                "duplicate",
                "{\"duplicate\":true}"
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> database.insertWhatsappOutboundMessage(
                inboundMessageId,
                farmId,
                userId,
                "corr-wa-flyway-1",
                "5511999990003",
                "text",
                "{\"text\":\"again\"}",
                "wamid.outbound.duplicate",
                "UNKNOWN",
                null
        )).isInstanceOf(DataIntegrityViolationException.class);

        database.insertVaccine(
                farmId,
                animalId,
                null,
                userId,
                "primeira",
                "app"
        );

        assertThatThrownBy(() -> database.insertVaccine(
                farmId,
                null,
                null,
                userId,
                "primeira",
                "app"
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> database.insertVaccine(
                farmId,
                animalId,
                lotId,
                userId,
                "primeira",
                "app"
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> database.insertVaccine(
                farmId,
                animalId,
                null,
                userId,
                "terceira",
                "app"
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> database.insertVaccine(
                farmId,
                animalId,
                null,
                userId,
                "primeira",
                "api"
        )).isInstanceOf(DataIntegrityViolationException.class);

        database.insertMedication(
                farmId,
                animalId,
                occurrenceId,
                userId,
                35,
                "app"
        );

        assertThatThrownBy(() -> database.insertMedication(
                farmId,
                animalId,
                occurrenceId,
                userId,
                -1,
                "app"
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> database.insertMedication(
                farmId,
                animalId,
                occurrenceId,
                userId,
                35,
                "api"
        )).isInstanceOf(DataIntegrityViolationException.class);

        database.insertFeeding(
                farmId,
                animalId,
                null,
                userId,
                "racao",
                "app"
        );

        assertThatThrownBy(() -> database.insertFeeding(
                farmId,
                null,
                null,
                userId,
                "racao",
                "app"
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> database.insertFeeding(
                farmId,
                animalId,
                lotId,
                userId,
                "racao",
                "app"
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> database.insertFeeding(
                farmId,
                animalId,
                null,
                userId,
                "desconhecido",
                "app"
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> database.insertFeeding(
                farmId,
                animalId,
                null,
                userId,
                "racao",
                "api"
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void farmScopedForeignKeysRejectCrossFarmRelationships() {
        TestDatabase database = migratedDatabase("tenancy");
        UUID farmA = database.insertFarm("Fazenda A");
        UUID farmB = database.insertFarm("Fazenda B");
        UUID userA = database.insertUser(farmA, "5511999990011", "User A", "admin", true);
        UUID userB = database.insertUser(farmB, "5511999990012", "User B", "worker", true);
        UUID animalA = database.insertAnimal(farmA, null, "BR-A-100", "macho", "ativo");
        UUID animalB = database.insertAnimal(farmB, null, "BR-B-100", "femea", "ativo");
        UUID lotB = database.insertLot(farmB, "Lote B", "raising");
        UUID damB = database.insertAnimal(farmB, null, null, null, "BR-B-001", "femea", "ativo");
        UUID sessionB = database.insertAuthSession(farmB, userB, null, null);

        // auth_sessions and refresh_tokens no longer have a composite FK through users(farm_id, id).
        // Cross-farm session isolation is enforced at the application layer via the membership
        // check in FarmSwitchService (V28 migration). The assertions below were testing DB-level
        // enforcement that has been intentionally removed to allow farm switching without
        // updating users.farm_id (which is blocked by other tables without ON UPDATE CASCADE).

        assertThatThrownBy(() -> database.insertAnimal(farmA, lotB, null, null, "BR-A-001", "macho", "ativo"))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> database.insertAnimal(farmA, null, damB, null, "BR-A-002", "femea", "ativo"))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> database.insertVaccine(
                farmA,
                animalB,
                null,
                userA,
                "primeira",
                "app"
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> database.insertVaccine(
                farmA,
                null,
                lotB,
                userA,
                "primeira",
                "app"
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> database.insertVaccine(
                farmA,
                animalA,
                null,
                userB,
                "primeira",
                "app"
        )).isInstanceOf(DataIntegrityViolationException.class);

        UUID occurrenceB = database.insertOccurrence(farmB, animalB, userB);

        assertThatThrownBy(() -> database.insertMedication(
                farmA,
                animalB,
                null,
                userA,
                35,
                "app"
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> database.insertMedication(
                farmA,
                animalA,
                occurrenceB,
                userA,
                35,
                "app"
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> database.insertMedication(
                farmA,
                animalA,
                null,
                userB,
                35,
                "app"
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> database.insertFeeding(
                farmA,
                animalB,
                null,
                userA,
                "racao",
                "app"
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> database.insertFeeding(
                farmA,
                null,
                lotB,
                userA,
                "racao",
                "app"
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> database.insertFeeding(
                farmA,
                animalA,
                null,
                userB,
                "racao",
                "app"
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void expectedCompoundIndexesExistForFarmScopedTables() {
        TestDatabase database = migratedDatabase("indexes");

        assertThat(database.indexColumns("idx_lots_farm_id")).containsExactly("farm_id");
        assertThat(database.indexColumns("idx_users_farm_id_role")).containsExactly("farm_id", "role");
        assertThat(database.indexColumns("idx_users_farm_id")).containsExactly("farm_id");
        assertThat(database.indexColumns("idx_users_farm_id_ativo")).containsExactly("farm_id", "ativo");
        assertThat(database.indexColumns("idx_lots_farm_id_finalidade")).containsExactly("farm_id", "finalidade");
        assertThat(database.indexColumns("idx_animals_farm_id")).containsExactly("farm_id");
        assertThat(database.indexColumns("idx_animals_farm_id_lot_id")).containsExactly("farm_id", "lot_id");
        assertThat(database.indexColumns("idx_animals_farm_id_status")).containsExactly("farm_id", "status");
        assertThat(database.indexColumns("idx_animals_farm_id_sexo")).containsExactly("farm_id", "sexo");
        assertThat(database.indexColumns("idx_auth_sessions_user_id")).containsExactly("user_id");
        assertThat(database.indexColumns("idx_auth_sessions_farm_id")).containsExactly("farm_id");
        assertThat(database.indexColumns("idx_refresh_tokens_session_id")).containsExactly("session_id");
        assertThat(database.indexColumns("idx_refresh_tokens_farm_id")).containsExactly("farm_id");
        assertThat(database.indexColumns("idx_refresh_tokens_user_id")).containsExactly("user_id");
        assertThat(database.indexColumns("idx_refresh_tokens_expires_at")).containsExactly("expires_at");
        assertThat(database.indexColumns("idx_occurrences_farm_id_animal_id_created_at_desc"))
                .containsExactly("farm_id", "animal_id", "created_at");
        assertThat(database.indexColumns("idx_occurrences_farm_id_status_gravidade"))
                .containsExactly("farm_id", "status", "gravidade");
        assertThat(database.indexColumns("idx_occurrences_farm_id_created_at_desc"))
                .containsExactly("farm_id", "created_at");
        assertThat(database.indexColumns("idx_vaccines_farm_id_animal_id_data_aplicacao_desc"))
                .containsExactly("farm_id", "animal_id", "data_aplicacao");
        assertThat(database.indexColumns("idx_vaccines_farm_id_lot_id_data_aplicacao_desc"))
                .containsExactly("farm_id", "lot_id", "data_aplicacao");
        assertThat(database.indexColumns("idx_vaccines_farm_id_data_proxima_dose"))
                .containsExactly("farm_id", "data_proxima_dose");
        assertThat(database.indexColumns("idx_medications_farm_id_animal_id_data_aplicacao_desc"))
                .containsExactly("farm_id", "animal_id", "data_aplicacao");
        assertThat(database.indexColumns("idx_medications_farm_id_release_date"))
                .containsExactly("farm_id", "release_date");
        assertThat(database.indexColumns("idx_medications_farm_id_occurrence_id"))
                .containsExactly("farm_id", "occurrence_id");
        assertThat(database.indexColumns("idx_alimentacoes_farm_id_animal_id_data_inicio_desc"))
                .containsExactly("farm_id", "animal_id", "data_inicio");
        assertThat(database.indexColumns("idx_alimentacoes_farm_id_lot_id_data_inicio_desc"))
                .containsExactly("farm_id", "lot_id", "data_inicio");
        assertThat(database.indexColumns("idx_alimentacoes_farm_id_data_inicio_desc"))
                .containsExactly("farm_id", "data_inicio");
        assertThat(database.indexColumns("idx_weighings_farm_id_animal_id_data_weighing_desc"))
                .containsExactly("farm_id", "animal_id", "data_weighing");
        assertThat(database.indexColumns("idx_weighings_farm_id_data_weighing_desc"))
                .containsExactly("farm_id", "data_weighing");
        assertThat(database.indexColumns("idx_weighings_farm_id_origem"))
                .containsExactly("farm_id", "origem");
        assertThat(database.indexColumns("idx_whatsapp_inbound_messages_farm_id_created_at")).containsExactly("farm_id", "created_at");
        assertThat(database.indexColumns("idx_whatsapp_inbound_messages_status")).containsExactly("status");
        assertThat(database.indexColumns("idx_whatsapp_inbound_messages_phone_number")).containsExactly("phone_number");
        assertThat(database.indexColumns("idx_whatsapp_processing_steps_inbound_message_id_created_at"))
                .containsExactly("inbound_message_id", "created_at");
        assertThat(database.indexColumns("idx_whatsapp_processing_steps_status")).containsExactly("status");
        assertThat(database.indexColumns("idx_whatsapp_audio_media_downloaded_at")).containsExactly("downloaded_at");
        assertThat(database.indexColumns("idx_whatsapp_audio_media_transcoded_at")).containsExactly("transcoded_at");
        assertThat(database.indexColumns("idx_whatsapp_transcripts_transcribed_at")).containsExactly("transcribed_at");
        assertThat(database.indexColumns("idx_media_references_owner")).containsExactly("owner_type", "owner_id");
        assertThat(database.indexColumns("idx_media_references_purpose")).containsExactly("purpose");
    }

    private static TestDatabase migratedDatabase(String suffix) {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:curralzap_" + suffix + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        return new TestDatabase(dataSource);
    }

    private record TestDatabase(JdbcTemplate jdbcTemplate) {

        TestDatabase(DataSource dataSource) {
            this(new JdbcTemplate(dataSource));
        }

        List<String> tableNames() {
            return jdbcTemplate.queryForList(
                    """
                            select table_name
                            from information_schema.tables
                            where table_schema = 'public'
                            order by table_name
                            """,
                    String.class
            );
        }

        List<String> indexColumns(String indexName) {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    """
                            select column_name
                            from information_schema.index_columns
                            where index_schema = 'public'
                              and index_name = ?
                            order by ordinal_position
                            """,
                    indexName
            );

            return rows.stream()
                    .map(row -> row.get("column_name").toString())
                    .toList();
        }

        UUID insertFarm(String nome) {
            UUID farmId = UUID.randomUUID();
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

            jdbcTemplate.update(
                    """
                            insert into farms (id, nome, rendimento_carcaca, created_at, updated_at)
                            values (?, ?, ?, ?, ?)
                            """,
                    farmId,
                    nome,
                    50.00,
                    Timestamp.from(now.toInstant()),
                    Timestamp.from(now.toInstant())
            );

            return farmId;
        }

        UUID insertLot(UUID farmId, String nome, String finalidade) {
            UUID lotId = UUID.randomUUID();
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

            jdbcTemplate.update(
                    """
                            insert into lots (id, farm_id, nome, finalidade, created_at, updated_at)
                            values (?, ?, ?, ?, ?, ?)
                            """,
                    lotId,
                    farmId,
                    nome,
                    finalidade,
                    Timestamp.from(now.toInstant()),
                    Timestamp.from(now.toInstant())
            );

            return lotId;
        }

        UUID insertUser(UUID farmId, String telefone, String nome, String role, boolean ativo) {
            UUID userId = UUID.randomUUID();
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

            jdbcTemplate.update(
                    """
                            insert into users (id, farm_id, telefone, nome, role, ativo, created_at, updated_at)
                            values (?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    userId,
                    farmId,
                    telefone,
                    nome,
                    role,
                    ativo,
                    Timestamp.from(now.toInstant()),
                    Timestamp.from(now.toInstant())
            );

            return userId;
        }

        UUID insertAnimal(UUID farmId, UUID lotId, String identificacao, String sexo, String status) {
            return insertAnimal(farmId, lotId, null, null, identificacao, sexo, status);
        }

        UUID insertAnimal(
                UUID farmId,
                UUID lotId,
                UUID damId,
                UUID sireId,
                String identificacao,
                String sexo,
                String status
        ) {
            UUID animalId = UUID.randomUUID();
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

            jdbcTemplate.update(
                    """
                            insert into animals (
                                id, farm_id, lot_id, identificacao, sexo, dam_id, sire_id, status, created_at, updated_at
                            )
                            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    animalId,
                    farmId,
                    lotId,
                    identificacao,
                    sexo,
                    damId,
                    sireId,
                    status,
                    Timestamp.from(now.toInstant()),
                    Timestamp.from(now.toInstant())
            );

            return animalId;
        }

        UUID insertVaccine(
                UUID farmId,
                UUID animalId,
                UUID lotId,
                UUID recordedBy,
                String dose,
                String source
        ) {
            UUID vaccineId = UUID.randomUUID();
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

            jdbcTemplate.update(
                    """
                            insert into vaccines (
                                id, farm_id, animal_id, lot_id, vaccine_type, vaccine_batch, dose, data_aplicacao,
                                data_proxima_dose, volume_ml, origem, registrado_por, created_at, updated_at
                            )
                            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    vaccineId,
                    farmId,
                    animalId,
                    lotId,
                    "Aftosa",
                    "BATCH-001",
                    dose,
                    Date.valueOf(LocalDate.of(2026, 4, 23)),
                    Date.valueOf(LocalDate.of(2026, 5, 23)),
                    new BigDecimal("2.50"),
                    source,
                    recordedBy,
                    Timestamp.from(now.toInstant()),
                    Timestamp.from(now.toInstant())
            );

            return vaccineId;
        }

        UUID insertOccurrence(UUID farmId, UUID animalId, UUID recordedBy) {
            UUID occurrenceId = UUID.randomUUID();
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

            jdbcTemplate.update(
                    """
                            insert into occurrences (
                                id, farm_id, animal_id, tipo, gravidade, descricao, foto_url, origem,
                                registrado_por, status, resolvido_por, created_at, updated_at
                            )
                            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
                    "open",
                    null,
                    Timestamp.from(now.toInstant()),
                    Timestamp.from(now.toInstant())
            );

            return occurrenceId;
        }

        UUID insertMedication(
                UUID farmId,
                UUID animalId,
                UUID occurrenceId,
                UUID recordedBy,
                int withdrawalPeriodDays,
                String source
        ) {
            UUID medicationId = UUID.randomUUID();
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            LocalDate applicationDate = LocalDate.of(2026, 4, 23);

            jdbcTemplate.update(
                    """
                            insert into medications (
                                id, farm_id, animal_id, occurrence_id, medication_name, principio_ativo, motivo,
                                dosagem, data_aplicacao, periodo_withdrawal, release_date, custo, origem,
                                registrado_por, created_at, updated_at
                            )
                            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    medicationId,
                    farmId,
                    animalId,
                    occurrenceId,
                    "Ivermectin",
                    "Ivermectina",
                    "Ticks",
                    "10 ml",
                    Date.valueOf(applicationDate),
                    withdrawalPeriodDays,
                    Date.valueOf(applicationDate.plusDays(withdrawalPeriodDays)),
                    new BigDecimal("45.90"),
                    source,
                    recordedBy,
                    Timestamp.from(now.toInstant()),
                    Timestamp.from(now.toInstant())
            );

            return medicationId;
        }

        UUID insertFeeding(
                UUID farmId,
                UUID animalId,
                UUID lotId,
                UUID recordedBy,
                String feedType,
                String source
        ) {
            UUID feedingId = UUID.randomUUID();
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

            jdbcTemplate.update(
                    """
                            insert into alimentacoes (
                                id, farm_id, animal_id, lot_id, tipo_alimento, descricao, quantidade_kg,
                                custo_por_kg, data_inicio, data_fim, origem, registrado_por, created_at, updated_at
                            )
                            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    feedingId,
                    farmId,
                    animalId,
                    lotId,
                    feedType,
                    "Descricao feed",
                    new BigDecimal("10.00"),
                    new BigDecimal("1.50"),
                    Date.valueOf(LocalDate.of(2026, 4, 23)),
                    null,
                    source,
                    recordedBy,
                    Timestamp.from(now.toInstant()),
                    Timestamp.from(now.toInstant())
            );

            return feedingId;
        }

        UUID insertAuthSession(UUID farmId, UUID userId, OffsetDateTime revokedAt, String revocationReason) {
            UUID sessionId = UUID.randomUUID();
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

            jdbcTemplate.update(
                    """
                            insert into auth_sessions (id, farm_id, user_id, revoked_at, revocation_reason, created_at, updated_at)
                            values (?, ?, ?, ?, ?, ?, ?)
                            """,
                    sessionId,
                    farmId,
                    userId,
                    revokedAt == null ? null : Timestamp.from(revokedAt.toInstant()),
                    revocationReason,
                    Timestamp.from(now.toInstant()),
                    Timestamp.from(now.toInstant())
            );

            return sessionId;
        }

        UUID insertWhatsappInboundMessage(
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
            UUID messageId = UUID.randomUUID();
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

            jdbcTemplate.update(
                    """
                            insert into whatsapp_inbound_messages (
                                id, provider_message_id, event_type, phone_number, user_id, farm_id,
                                correlation_id, message_type, raw_payload, status, failure_reason, received_at, created_at, updated_at
                            )
                            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    messageId,
                    providerMessageId,
                    eventType,
                    phoneNumber,
                    userId,
                    farmId,
                    correlationId,
                    messageType,
                    "{\"event\":\"messages.upsert\"}",
                    status,
                    failureReason,
                    Timestamp.from(now.toInstant()),
                    Timestamp.from(now.toInstant()),
                    Timestamp.from(now.toInstant())
            );

            return messageId;
        }

        UUID insertWhatsappInboundMessage(
                String providerMessageId,
                String phoneNumber,
                String eventType,
                String messageType,
                String status
        ) {
            return insertWhatsappInboundMessage(
                    providerMessageId,
                    phoneNumber,
                    null,
                    null,
                    null,
                    eventType,
                    messageType,
                    status,
                    null
            );
        }

        UUID insertWhatsappProcessingStep(UUID inboundMessageId, String status, String details) {
            UUID stepId = UUID.randomUUID();
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

            jdbcTemplate.update(
                    """
                            insert into whatsapp_processing_steps (
                                id, inbound_message_id, status, details, occurred_at, created_at, updated_at
                            )
                            values (?, ?, ?, ?, ?, ?, ?)
                            """,
                    stepId,
                    inboundMessageId,
                    status,
                    details,
                    Timestamp.from(now.toInstant()),
                    Timestamp.from(now.toInstant()),
                    Timestamp.from(now.toInstant())
            );

            return stepId;
        }

        UUID insertWhatsappAudioMedia(
                UUID inboundMessageId,
                String sourceUrl,
                String mimeType,
                String storagePath,
                String transcodedMimeType,
                String transcodedStoragePath,
                OffsetDateTime transcodedAt
        ) {
            UUID audioMediaId = UUID.randomUUID();
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

            jdbcTemplate.update(
                    """
                            insert into whatsapp_audio_media (
                                id, inbound_message_id, source_url, mime_type, storage_path, downloaded_at,
                                transcoded_mime_type, transcoded_storage_path, transcoded_at, created_at, updated_at
                            )
                            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    audioMediaId,
                    inboundMessageId,
                    sourceUrl,
                    mimeType,
                    storagePath,
                    Timestamp.from(now.toInstant()),
                    transcodedMimeType,
                    transcodedStoragePath,
                    transcodedAt == null ? null : Timestamp.from(transcodedAt.toInstant()),
                    Timestamp.from(now.toInstant()),
                    Timestamp.from(now.toInstant())
            );

            return audioMediaId;
        }

        UUID insertMediaReference(
                String ownerType,
                UUID ownerId,
                String purpose,
                String externalUrl,
                String storagePath,
                String mimeType,
                Long fileSizeBytes,
                String checksumSha256
        ) {
            UUID mediaReferenceId = UUID.randomUUID();
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

            jdbcTemplate.update(
                    """
                            insert into media_references (
                                id, owner_type, owner_id, purpose, external_url, storage_path, mime_type,
                                file_size_bytes, checksum_sha256, created_at, updated_at
                            )
                            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    mediaReferenceId,
                    ownerType,
                    ownerId,
                    purpose,
                    externalUrl,
                    storagePath,
                    mimeType,
                    fileSizeBytes,
                    checksumSha256,
                    Timestamp.from(now.toInstant()),
                    Timestamp.from(now.toInstant())
            );

            return mediaReferenceId;
        }

        UUID insertWhatsappTranscript(
                UUID inboundMessageId,
                String provider,
                String providerModel,
                String transcript
        ) {
            UUID transcriptId = UUID.randomUUID();
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

            jdbcTemplate.update(
                    """
                            insert into whatsapp_transcripts (
                                id, inbound_message_id, provider, provider_model, transcript, transcribed_at, created_at, updated_at
                            )
                            values (?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    transcriptId,
                    inboundMessageId,
                    provider,
                    providerModel,
                    transcript,
                    Timestamp.from(now.toInstant()),
                    Timestamp.from(now.toInstant()),
                    Timestamp.from(now.toInstant())
            );

            return transcriptId;
        }

        UUID insertWhatsappExtractedPayload(
                UUID inboundMessageId,
                String provider,
                String providerModel,
                String intent,
                String structuredPayload
        ) {
            UUID extractedPayloadId = UUID.randomUUID();
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

            jdbcTemplate.update(
                    """
                            insert into whatsapp_extracted_payloads (
                                id, inbound_message_id, provider, provider_model, intent, structured_payload,
                                extracted_at, created_at, updated_at
                            )
                            values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    extractedPayloadId,
                    inboundMessageId,
                    provider,
                    providerModel,
                    intent,
                    structuredPayload,
                    Timestamp.from(now.toInstant()),
                    Timestamp.from(now.toInstant()),
                    Timestamp.from(now.toInstant())
            );

            return extractedPayloadId;
        }

        UUID insertWhatsappOutboundMessage(
                UUID inboundMessageId,
                UUID farmId,
                UUID userId,
                String correlationId,
                String phoneNumber,
                String messageType,
                String payload,
                String providerMessageId,
                String status,
                String failureReason
        ) {
            UUID outboundMessageId = UUID.randomUUID();
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

            jdbcTemplate.update(
                    """
                            insert into whatsapp_outbound_messages (
                                id, inbound_message_id, farm_id, user_id, correlation_id, phone_number, message_type,
                                payload, provider_message_id, status, failure_reason, queued_at, sent_at, created_at, updated_at
                            )
                            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    outboundMessageId,
                    inboundMessageId,
                    farmId,
                    userId,
                    correlationId,
                    phoneNumber,
                    messageType,
                    payload,
                    providerMessageId,
                    status,
                    failureReason,
                    Timestamp.from(now.toInstant()),
                    null,
                    Timestamp.from(now.toInstant()),
                    Timestamp.from(now.toInstant())
            );

            return outboundMessageId;
        }

        UUID insertRefreshToken(
                UUID sessionId,
                UUID farmId,
                UUID userId,
                OffsetDateTime consumedAt,
                OffsetDateTime revokedAt,
                String revocationReason,
                UUID replacedByTokenId
        ) {
            UUID tokenId = UUID.randomUUID();
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

            jdbcTemplate.update(
                    """
                            insert into refresh_tokens (
                                id, session_id, farm_id, user_id, expires_at, consumed_at, revoked_at,
                                revocation_reason, replaced_by_token_id, created_at, updated_at
                            )
                            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    tokenId,
                    sessionId,
                    farmId,
                    userId,
                    Timestamp.from(now.plusDays(30).toInstant()),
                    consumedAt == null ? null : Timestamp.from(consumedAt.toInstant()),
                    revokedAt == null ? null : Timestamp.from(revokedAt.toInstant()),
                    revocationReason,
                    replacedByTokenId,
                    Timestamp.from(now.toInstant()),
                    Timestamp.from(now.toInstant())
            );

            return tokenId;
        }
    }
}
