package br.com.curralzap.backend.occurrences.api;

import br.com.curralzap.backend.support.BackendApiIntegrationTestSupport;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OccurrencesApiIntegrationTest extends BackendApiIntegrationTestSupport {

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"ADMIN", "VETERINARIAN", "WORKER"})
    void authorizedRolesCanListOccurrencesWithFiltersAndDenseContext(UserRole role) throws Exception {
        UUID farmId = insertFarm("Fazenda Ocorrencias Lista");
        UUID animalOneId = insertAnimal(farmId, null, "O-101", "Trovao", "Nelore", "macho", "ativo", null, null, null);
        UUID animalTwoId = insertAnimal(farmId, null, "O-102", "Aurora", "Nelore", "femea", "ativo", null, null, null);
        UserAccount user = insertUser(farmId, phoneFor(role) + "1", "Leitor " + role.name(), role, true);
        UUID resolvedOccurrenceId = insertOccurrence(farmId, animalTwoId, user.id(), "resolved", "critical", user.id(), "Fence repaired.");
        UUID openOccurrenceId = insertOccurrence(farmId, animalOneId, user.id(), "open", "high", null, "Animal limping.");
        insertOccurrence(farmId, animalOneId, user.id(), "in_follow_up", "medium", null, "Observation in progress.");
        String accessToken = issueAccessToken(user, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/occurrences")
                        .param("status", "open")
                        .param("severity", "high")
                        .param("limit", "1")
                        .param("offset", "0")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.limit").value(1))
                .andExpect(jsonPath("$.offset").value(0))
                .andExpect(jsonPath("$.items[0].id").value(openOccurrenceId.toString()))
                .andExpect(jsonPath("$.items[0].animalId").value(animalOneId.toString()))
                .andExpect(jsonPath("$.items[0].animalIdentifier").value("O-101"))
                .andExpect(jsonPath("$.items[0].animalName").value("Trovao"))
                .andExpect(jsonPath("$.items[0].type").value("health"))
                .andExpect(jsonPath("$.items[0].severity").value("high"))
                .andExpect(jsonPath("$.items[0].status").value("open"))
                .andExpect(jsonPath("$.items[0].description").value("Animal limping."))
                .andExpect(jsonPath("$.items[0].resolvedBy").doesNotExist());

        mockMvc.perform(get("/api/v1/occurrences")
                        .param("animalId", animalTwoId.toString())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value(resolvedOccurrenceId.toString()))
                .andExpect(jsonPath("$.items[0].status").value("resolved"))
                .andExpect(jsonPath("$.items[0].severity").value("critical"))
                .andExpect(jsonPath("$.items[0].resolvedBy").value(user.id().toString()));
    }

    @Test
    void invalidOccurrenceListQueryReturnsFieldErrors() throws Exception {
        UUID farmId = insertFarm("Fazenda Consulta Invalida");
        UserAccount admin = insertUser(farmId, "5511999997400", "Admin Consulta", UserRole.ADMIN, true);
        String accessToken = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/occurrences")
                        .param("limit", "0")
                        .param("offset", "-1")
                        .param("animalId", "not-a-uuid")
                        .param("status", "closed")
                        .param("severity", "urgent")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors.limit").value("Limit must be between 1 and 100."))
                .andExpect(jsonPath("$.fieldErrors.offset").value("Offset must be zero or greater."))
                .andExpect(jsonPath("$.fieldErrors.animalId").value("Animal id must be a valid UUID."))
                .andExpect(jsonPath("$.fieldErrors.status").value("Status is invalid."))
                .andExpect(jsonPath("$.fieldErrors.severity").value("Severity is invalid."));
    }

    @Test
    void cannotListOccurrencesForAnimalFromAnotherFarm() throws Exception {
        UUID farmId = insertFarm("Fazenda Lista Um");
        UserAccount admin = insertUser(farmId, "5511999997401", "Admin Lista", UserRole.ADMIN, true);
        UUID otherFarmId = insertFarm("Fazenda Lista Dois");
        UUID otherAnimalId = insertAnimal(otherFarmId, null, "O-998", "Outro", "Nelore", "macho", "ativo", null, null, null);
        String accessToken = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/occurrences")
                        .param("animalId", otherAnimalId.toString())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not_found"));
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"ADMIN", "VETERINARIAN"})
    void adminAndVeterinarianCanResolveOpenOccurrences(UserRole role) throws Exception {
        UUID farmId = insertFarm("Fazenda Ocorrencias");
        UUID animalId = insertAnimal(farmId, null, "O-001");
        UserAccount user = insertUser(farmId, phoneFor(role), "Resolvedor " + role.name(), role, true);
        UUID occurrenceId = insertOccurrence(farmId, animalId, user.id(), "open", null);
        String accessToken = issueAccessToken(user, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(post("/api/v1/occurrences/{occurrenceId}/resolve", occurrenceId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "note": "Treatment completed."
                                        }
                                        """
                        ))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.id").value(occurrenceId.toString()))
                .andExpect(jsonPath("$.status").value("resolved"))
                .andExpect(jsonPath("$.resolvedBy").value(user.id().toString()))
                .andExpect(jsonPath("$.resolvedAt", notNullValue()));

        assertEquals(
                "resolved",
                jdbcTemplate.queryForObject("select status from occurrences where id = ?", String.class, occurrenceId)
        );
        assertEquals(
                user.id().toString(),
                jdbcTemplate.queryForObject("select resolvido_por from occurrences where id = ?", String.class, occurrenceId)
        );
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"ADMIN", "VETERINARIAN"})
    void adminAndVeterinarianCanReopenResolvedOccurrences(UserRole role) throws Exception {
        UUID farmId = insertFarm("Fazenda Reabrir Ocorrencias");
        UUID animalId = insertAnimal(farmId, null, "O-002");
        UserAccount user = insertUser(farmId, phoneFor(role) + "9", "Reabridor " + role.name(), role, true);
        UUID occurrenceId = insertOccurrence(farmId, animalId, user.id(), "resolved", user.id());
        String accessToken = issueAccessToken(user, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(post("/api/v1/occurrences/{occurrenceId}/reopen", occurrenceId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.id").value(occurrenceId.toString()))
                .andExpect(jsonPath("$.status").value("open"));

        assertEquals(
                "open",
                jdbcTemplate.queryForObject("select status from occurrences where id = ?", String.class, occurrenceId)
        );
        assertNull(
                jdbcTemplate.queryForObject("select resolvido_por from occurrences where id = ?", Object.class, occurrenceId)
        );
    }

    @Test
    void workerCannotResolveOrReopenOccurrences() throws Exception {
        UUID farmId = insertFarm("Fazenda Worker Bloqueado");
        UUID animalId = insertAnimal(farmId, null, "O-003");
        UserAccount worker = insertUser(farmId, "5511999997100", "Worker Bloqueado", UserRole.WORKER, true);
        UUID openOccurrenceId = insertOccurrence(farmId, animalId, worker.id(), "open", null);
        UUID resolvedOccurrenceId = insertOccurrence(farmId, animalId, worker.id(), "resolved", worker.id());
        String accessToken = issueAccessToken(worker, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(post("/api/v1/occurrences/{occurrenceId}/resolve", openOccurrenceId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"note\":\"not allowed\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));

        mockMvc.perform(post("/api/v1/occurrences/{occurrenceId}/reopen", resolvedOccurrenceId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
    }

    @Test
    void cannotResolveOrReopenOccurrenceFromAnotherFarm() throws Exception {
        UUID farmId = insertFarm("Fazenda Um");
        UserAccount admin = insertUser(farmId, "5511999997200", "Admin Um", UserRole.ADMIN, true);
        UUID otherFarmId = insertFarm("Fazenda Dois");
        UUID animalId = insertAnimal(otherFarmId, null, "O-999");
        UserAccount foreignUser = insertUser(otherFarmId, "5511999997201", "Admin Dois", UserRole.ADMIN, true);
        UUID occurrenceId = insertOccurrence(otherFarmId, animalId, foreignUser.id(), "open", null);
        String accessToken = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(post("/api/v1/occurrences/{occurrenceId}/resolve", occurrenceId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"note\":\"cross farm\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not_found"));

        mockMvc.perform(post("/api/v1/occurrences/{occurrenceId}/reopen", occurrenceId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not_found"));
    }

    @Test
    void invalidStateTransitionsReturnConflict() throws Exception {
        UUID farmId = insertFarm("Fazenda Estado Invalido");
        UUID animalId = insertAnimal(farmId, null, "O-004");
        UserAccount admin = insertUser(farmId, "5511999997300", "Admin Estado", UserRole.ADMIN, true);
        UUID resolvedOccurrenceId = insertOccurrence(farmId, animalId, admin.id(), "resolved", admin.id());
        UUID openOccurrenceId = insertOccurrence(farmId, animalId, admin.id(), "open", null);
        String accessToken = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(post("/api/v1/occurrences/{occurrenceId}/resolve", resolvedOccurrenceId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"note\":\"duplicate resolve\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("invalid_state_transition"));

        mockMvc.perform(post("/api/v1/occurrences/{occurrenceId}/reopen", openOccurrenceId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("invalid_state_transition"));
    }

    @Test
    void authenticationIsRequiredForOccurrenceActions() throws Exception {
        mockMvc.perform(post("/api/v1/occurrences/{occurrenceId}/resolve", UUID.randomUUID())
                        .contentType(APPLICATION_JSON)
                        .content("{\"note\":\"missing auth\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication_required"));
    }

    private UUID insertOccurrence(
            UUID farmId,
            UUID animalId,
            UUID recordedBy,
            String status,
            UUID resolvedBy
    ) {
        return insertOccurrence(farmId, animalId, recordedBy, status, "high", resolvedBy, "Animal limping.");
    }

    private UUID insertOccurrence(
            UUID farmId,
            UUID animalId,
            UUID recordedBy,
            String status,
            String severity,
            UUID resolvedBy,
            String description
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
                severity,
                description,
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

    private String phoneFor(UserRole role) {
        return switch (role) {
            case ADMIN -> "5511999997001";
            case VETERINARIAN -> "5511999997002";
            case WORKER -> "5511999997003";
        };
    }
}
