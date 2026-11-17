package br.com.curralzap.backend.feeding.api;

import br.com.curralzap.backend.support.BackendApiIntegrationTestSupport;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

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

class FeedingsApiIntegrationTest extends BackendApiIntegrationTestSupport {

    @Test
    void adminCanCreateAnimalFeeding() throws Exception {
        UUID farmId = insertFarm("Fazenda Alimentacao Animal");
        UUID animalId = insertAnimal(farmId, null, "FED-A-001");
        UserAccount admin = insertUser(farmId, "5511999999401", "Feeding Admin", UserRole.ADMIN, true);
        String accessToken = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(post("/api/v1/feedings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "animalId": "%s",
                                          "feedType": "racao",
                                          "description": "Morning ration",
                                          "quantityKg": "8.50",
                                          "costPerKg": "2.10",
                                          "startDate": "2026-04-24",
                                          "endDate": "2026-04-30"
                                        }
                                        """.formatted(animalId)
                        ))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.animalId").value(animalId.toString()))
                .andExpect(jsonPath("$.lotId").doesNotExist())
                .andExpect(jsonPath("$.feedType").value("racao"))
                .andExpect(jsonPath("$.description").value("Morning ration"))
                .andExpect(jsonPath("$.quantityKg").value(8.50))
                .andExpect(jsonPath("$.costPerKg").value(2.10))
                .andExpect(jsonPath("$.startDate").value("2026-04-24"))
                .andExpect(jsonPath("$.endDate").value("2026-04-30"))
                .andExpect(jsonPath("$.source").value("app"))
                .andExpect(jsonPath("$.recordedBy").value(admin.id().toString()));

        String persistedType = jdbcTemplate.queryForObject(
                "select tipo_alimento from alimentacoes where farm_id = ? and animal_id = ?",
                String.class,
                farmId,
                animalId
        );

        assertEquals("racao", persistedType);
    }

    @Test
    void adminCanCreateLotFeeding() throws Exception {
        UUID farmId = insertFarm("Fazenda Alimentacao Lote");
        UUID lotId = insertLot(farmId, "Recria", "raising");
        UserAccount admin = insertUser(farmId, "5511999999402", "Feeding Lot Admin", UserRole.ADMIN, true);
        String accessToken = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(post("/api/v1/feedings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "lotId": "%s",
                                          "feedType": "silagem",
                                          "quantityKg": "120.00",
                                          "startDate": "2026-04-20"
                                        }
                                        """.formatted(lotId)
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.animalId").doesNotExist())
                .andExpect(jsonPath("$.lotId").value(lotId.toString()))
                .andExpect(jsonPath("$.feedType").value("silagem"))
                .andExpect(jsonPath("$.quantityKg").value(120.00));
    }

    @Test
    void invalidCreatePayloadReturnsFieldErrors() throws Exception {
        UUID farmId = insertFarm("Fazenda Alimentacao Invalida");
        UserAccount admin = insertUser(farmId, "5511999999403", "Feeding Invalid Admin", UserRole.ADMIN, true);
        String accessToken = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(post("/api/v1/feedings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "animalId": "not-a-uuid",
                                          "lotId": "also-not-a-uuid",
                                          "feedType": "banana",
                                          "description": "%s",
                                          "quantityKg": "-1.001",
                                          "costPerKg": "-2.001",
                                          "startDate": "2026-99-24",
                                          "endDate": "2026-99-01"
                                        }
                                        """.formatted("x".repeat(1001))
                        ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors.target").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.feedType").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.description").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.quantityKg").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.costPerKg").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.startDate").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.endDate").isNotEmpty());
    }

    @Test
    void returnsNotFoundWhenCreateTargetIsOutsideUsersFarm() throws Exception {
        UUID farmId = insertFarm("Fazenda Alimentacao");
        UserAccount admin = insertUser(farmId, "5511999999404", "Feeding Admin", UserRole.ADMIN, true);
        String accessToken = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));

        UUID otherFarmId = insertFarm("Outra Fazenda Alimentacao");
        UUID foreignAnimalId = insertAnimal(otherFarmId, null, "FED-FOREIGN");

        mockMvc.perform(post("/api/v1/feedings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "animalId": "%s",
                                          "feedType": "racao",
                                          "quantityKg": "8.50",
                                          "startDate": "2026-04-24"
                                        }
                                        """.formatted(foreignAnimalId)
                        ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not_found"));
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"VETERINARIAN", "WORKER"})
    void nonAdminRolesCannotCreateOrListFeedings(UserRole role) throws Exception {
        UUID farmId = insertFarm("Fazenda Role Feeding");
        UUID animalId = insertAnimal(farmId, null, "FED-R-001");
        UserAccount user = insertUser(farmId, phoneFor(role), "Feeding " + role.name(), role, true);
        String accessToken = issueAccessToken(user, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(post("/api/v1/feedings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "animalId": "%s",
                                          "feedType": "racao",
                                          "quantityKg": "8.50",
                                          "startDate": "2026-04-24"
                                        }
                                        """.formatted(animalId)
                        ))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));

        mockMvc.perform(get("/api/v1/feedings")
                        .param("animalId", animalId.toString())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
    }

    @Test
    void listsAnimalFeedingsWithStableOffsetPagination() throws Exception {
        UUID farmId = insertFarm("Fazenda Historico Feeding Animal");
        UUID animalId = insertAnimal(farmId, null, "FED-H-001");
        UserAccount admin = insertUser(farmId, "5511999999405", "Feeding History Admin", UserRole.ADMIN, true);
        insertHistoricalFeeding(farmId, animalId, null, admin.id(), "racao", "Feed A", "8.50", "2.10", LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 10));
        insertHistoricalFeeding(farmId, animalId, null, admin.id(), "silagem", "Feed B", "10.00", "1.90", LocalDate.of(2026, 4, 15), null);
        insertHistoricalFeeding(farmId, animalId, null, admin.id(), "suplemento", "Feed C", "3.00", null, LocalDate.of(2026, 4, 20), null);

        String accessToken = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/feedings")
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
                .andExpect(jsonPath("$.items[0].feedType").value("suplemento"))
                .andExpect(jsonPath("$.items[0].startDate").value("2026-04-20"))
                .andExpect(jsonPath("$.items[1].feedType").value("silagem"));
    }

    @Test
    void listsLotFeedings() throws Exception {
        UUID farmId = insertFarm("Fazenda Historico Feeding Lote");
        UUID lotId = insertLot(farmId, "Engorda", "fattening");
        UserAccount admin = insertUser(farmId, "5511999999406", "Feeding Lot Admin", UserRole.ADMIN, true);
        insertHistoricalFeeding(farmId, null, lotId, admin.id(), "misto", "Mixed feed", "120.00", "1.10", LocalDate.of(2026, 4, 10), null);
        insertHistoricalFeeding(farmId, null, lotId, admin.id(), "pasto", "Pasture", "80.00", null, LocalDate.of(2026, 4, 2), LocalDate.of(2026, 4, 9));

        String accessToken = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/feedings")
                        .param("lotId", lotId.toString())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items[0].lotId").value(lotId.toString()))
                .andExpect(jsonPath("$.items[0].feedType").value("misto"))
                .andExpect(jsonPath("$.items[1].feedType").value("pasto"));
    }

    @Test
    void returnsNotFoundWhenListTargetIsOutsideUsersFarm() throws Exception {
        UUID farmId = insertFarm("Fazenda Lista Feeding");
        UserAccount admin = insertUser(farmId, "5511999999407", "Feeding List Admin", UserRole.ADMIN, true);
        String accessToken = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));

        UUID otherFarmId = insertFarm("Outra Fazenda Lista Feeding");
        UUID foreignAnimalId = insertAnimal(otherFarmId, null, "FED-LIST-FOREIGN");
        UUID foreignLotId = insertLot(otherFarmId, "Outro Lote", "raising");

        mockMvc.perform(get("/api/v1/feedings")
                        .param("animalId", foreignAnimalId.toString())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not_found"));

        mockMvc.perform(get("/api/v1/feedings")
                        .param("lotId", foreignLotId.toString())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not_found"));
    }

    @Test
    void invalidListQueryReturnsFieldErrors() throws Exception {
        UUID farmId = insertFarm("Fazenda Lista Feeding Invalida");
        UserAccount admin = insertUser(farmId, "5511999999408", "Feeding List Invalid Admin", UserRole.ADMIN, true);
        String accessToken = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/feedings")
                        .param("animalId", "not-a-uuid")
                        .param("lotId", "also-not-a-uuid")
                        .param("limit", "0")
                        .param("offset", "-1")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors.target").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.limit").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.offset").isNotEmpty());
    }

    @Test
    void authenticationIsRequiredForFeedingEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/feedings"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication_required"));
    }

    private String phoneFor(UserRole role) {
        return switch (role) {
            case ADMIN -> "5511999999491";
            case VETERINARIAN -> "5511999999492";
            case WORKER -> "5511999999493";
        };
    }

    private void insertHistoricalFeeding(
            UUID farmId,
            UUID animalId,
            UUID lotId,
            UUID recordedBy,
            String feedType,
            String description,
            String quantityKg,
            String costPerKg,
            LocalDate startDate,
            LocalDate endDate
    ) {
        UUID feedingId = UUID.randomUUID();
        OffsetDateTime now = nowUtc();

        jdbcTemplate.update(
                """
                        insert into alimentacoes (
                            id, farm_id, animal_id, lot_id, tipo_alimento, descricao, quantidade_kg,
                            custo_por_kg, data_inicio, data_fim, origem, registrado_por, created_at, updated_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                feedingId,
                farmId,
                animalId,
                lotId,
                feedType,
                description,
                new BigDecimal(quantityKg),
                costPerKg != null ? new BigDecimal(costPerKg) : null,
                Date.valueOf(startDate),
                endDate != null ? Date.valueOf(endDate) : null,
                "app",
                recordedBy,
                Timestamp.from(now.toInstant()),
                Timestamp.from(now.toInstant())
        );
    }
}
