package br.com.curralzap.backend.weighings.api;

import br.com.curralzap.backend.support.BackendApiIntegrationTestSupport;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserRole;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WeighingsApiIntegrationTest extends BackendApiIntegrationTestSupport {

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"ADMIN", "VETERINARIAN", "WORKER"})
    void allowedRolesCanCreateWeighingAndReceiveGmdWhenPriorExists(UserRole role) throws Exception {
        UUID farmId = insertFarm("Fazenda Pesagens");
        UUID animalId = insertAnimal(farmId, null, "W-001");
        UserAccount user = insertUser(farmId, phoneFor(role), "Pesagem " + role.name(), role, true);
        insertHistoricalWeighing(
                farmId,
                animalId,
                new BigDecimal("500.00"),
                LocalDate.of(2026, 3, 10),
                user.id()
        );

        String accessToken = issueAccessToken(user, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(post("/api/v1/weighings")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Idempotency-Key", "weighing-" + role.name().toLowerCase())
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "animalId": "%s",
                                          "weightKg": 507.49,
                                          "weighingDate": "2026-03-17",
                                          "type": "routine"
                                        }
                                        """.formatted(animalId)
                        ))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.animalId").value(animalId.toString()))
                .andExpect(jsonPath("$.weightKg").value(507.49))
                .andExpect(jsonPath("$.weighingDate").value("2026-03-17"))
                .andExpect(jsonPath("$.type").value("routine"))
                .andExpect(jsonPath("$.source").value("app"))
                .andExpect(jsonPath("$.recordedBy").value(user.id().toString()))
                .andExpect(jsonPath("$.gmdKgPerDay").value(1.07));

        Integer weighingCount = jdbcTemplate.queryForObject("select count(*) from weighings where farm_id = ?", Integer.class, farmId);
        assertEquals(2, weighingCount);
    }

    @Test
    void invalidPayloadReturnsFieldErrors() throws Exception {
        UUID farmId = insertFarm("Fazenda Pesagem Invalida");
        UserAccount user = insertUser(farmId, "5511999996100", "Pesagem Admin", UserRole.ADMIN, true);
        String accessToken = issueAccessToken(user, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(post("/api/v1/weighings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "animalId": "not-a-uuid",
                                          "weightKg": "-1.001",
                                          "weighingDate": "2026-99-17",
                                          "type": "unexpected"
                                        }
                                        """
                        ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors.animalId").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.weightKg").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.weighingDate").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.type").isNotEmpty());
    }

    @Test
    void requiresAuthenticationToCreateWeighing() throws Exception {
        mockMvc.perform(post("/api/v1/weighings")
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "animalId": "00000000-0000-0000-0000-000000000000",
                                          "weightKg": 500.00,
                                          "weighingDate": "2026-03-17",
                                          "type": "routine"
                                        }
                                        """
                        ))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication_required"));
    }

    @Test
    void cannotCreateWeighingForAnimalFromAnotherFarm() throws Exception {
        UUID farmId = insertFarm("Fazenda Pesagem");
        UserAccount user = insertUser(farmId, "5511999996200", "Pesagem Admin", UserRole.ADMIN, true);
        UUID otherFarmId = insertFarm("Outra Fazenda Pesagem");
        UUID foreignAnimalId = insertAnimal(otherFarmId, null, "W-999");
        String accessToken = issueAccessToken(user, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(post("/api/v1/weighings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "animalId": "%s",
                                          "weightKg": 520.00,
                                          "weighingDate": "2026-03-17",
                                          "type": "routine"
                                        }
                                        """.formatted(foreignAnimalId)
                        ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not_found"));
    }

    @Test
    void duplicateSubmissionWithSameIdempotencyKeyReturnsExistingWeighing() throws Exception {
        UUID farmId = insertFarm("Fazenda Idempotencia");
        UUID animalId = insertAnimal(farmId, null, "W-123");
        UserAccount user = insertUser(farmId, "5511999996300", "Pesagem Admin", UserRole.ADMIN, true);
        String accessToken = issueAccessToken(user, OffsetDateTime.now(ZoneOffset.UTC));

        String body =
                """
                        {
                          "animalId": "%s",
                          "weightKg": 510.25,
                          "weighingDate": "2026-03-17",
                          "type": "routine"
                        }
                        """.formatted(animalId);

        String firstResponse = mockMvc.perform(post("/api/v1/weighings")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Idempotency-Key", "dup-weighing-key")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String secondResponse = mockMvc.perform(post("/api/v1/weighings")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Idempotency-Key", "dup-weighing-key")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertEquals(firstResponse, secondResponse);

        Integer weighingCount = jdbcTemplate.queryForObject("select count(*) from weighings where farm_id = ?", Integer.class, farmId);
        assertEquals(1, weighingCount);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"ADMIN", "VETERINARIAN", "WORKER"})
    void allowedRolesCanListAnimalWeighingHistoryWithStablePaginationAndGmd(UserRole role) throws Exception {
        UUID farmId = insertFarm("Fazenda Historico Pesagens");
        UUID animalId = insertAnimal(farmId, null, "W-LIST-001");
        UserAccount user = insertUser(farmId, phoneFor(role), "Historico " + role.name(), role, true);
        insertHistoricalWeighing(farmId, animalId, new BigDecimal("500.00"), LocalDate.of(2026, 3, 10), user.id());
        insertHistoricalWeighing(farmId, animalId, new BigDecimal("507.49"), LocalDate.of(2026, 3, 17), user.id());
        insertHistoricalWeighing(farmId, animalId, new BigDecimal("511.49"), LocalDate.of(2026, 3, 21), user.id());

        String accessToken = issueAccessToken(user, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/weighings")
                        .param("animalId", animalId.toString())
                        .param("limit", "2")
                        .param("offset", "0")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.limit").value(2))
                .andExpect(jsonPath("$.offset").value(0))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].weightKg").value(511.49))
                .andExpect(jsonPath("$.items[0].weighingDate").value("2026-03-21"))
                .andExpect(jsonPath("$.items[0].gmdKgPerDay").value(1.00))
                .andExpect(jsonPath("$.items[1].weightKg").value(507.49))
                .andExpect(jsonPath("$.items[1].gmdKgPerDay").value(1.07));
    }

    @Test
    void invalidListQueryReturnsFieldErrors() throws Exception {
        UUID farmId = insertFarm("Fazenda Lista Pesagens Invalida");
        UserAccount user = insertUser(farmId, "5511999996400", "Lista Pesagem Admin", UserRole.ADMIN, true);
        String accessToken = issueAccessToken(user, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/weighings")
                        .param("animalId", "not-a-uuid")
                        .param("limit", "0")
                        .param("offset", "-1")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors.animalId").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.limit").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.offset").isNotEmpty());
    }

    @Test
    void cannotListWeighingsForAnimalFromAnotherFarm() throws Exception {
        UUID farmId = insertFarm("Fazenda Lista Pesagens");
        UserAccount user = insertUser(farmId, "5511999996500", "Lista Pesagem Admin", UserRole.ADMIN, true);
        UUID otherFarmId = insertFarm("Outra Fazenda Lista Pesagens");
        UUID foreignAnimalId = insertAnimal(otherFarmId, null, "W-LIST-999");
        String accessToken = issueAccessToken(user, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/weighings")
                        .param("animalId", foreignAnimalId.toString())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not_found"));
    }

    private String phoneFor(UserRole role) {
        return switch (role) {
            case ADMIN -> "5511999996001";
            case VETERINARIAN -> "5511999996002";
            case WORKER -> "5511999996003";
        };
    }

    private void insertHistoricalWeighing(
            UUID farmId,
            UUID animalId,
            BigDecimal weightKg,
            LocalDate weighingDate,
            UUID recordedBy
    ) {
        UUID weighingId = UUID.randomUUID();
        OffsetDateTime now = nowUtc();

        jdbcTemplate.update(
                """
                        insert into weighings (
                            id, farm_id, animal_id, peso_kg, data_weighing, tipo, origem, registrado_por,
                            idempotency_key, audio_transcricao, created_at, updated_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                weighingId,
                farmId,
                animalId,
                weightKg,
                Date.valueOf(weighingDate),
                "rotina",
                "app",
                recordedBy,
                null,
                null,
                Timestamp.from(now.toInstant()),
                Timestamp.from(now.toInstant())
        );
    }
}
