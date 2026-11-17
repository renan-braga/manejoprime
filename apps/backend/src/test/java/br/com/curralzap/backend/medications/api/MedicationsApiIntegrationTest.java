package br.com.curralzap.backend.medications.api;

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

class MedicationsApiIntegrationTest extends BackendApiIntegrationTestSupport {

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"ADMIN", "VETERINARIAN"})
    void adminAndVeterinarianCanCreateMedication(UserRole role) throws Exception {
        UUID farmId = insertFarm("Fazenda Medicacao");
        UUID animalId = insertAnimal(farmId, null, "MED-A-001");
        UserAccount user = insertUser(farmId, phoneFor(role), "Medicacao " + role.name(), role, true);
        UUID occurrenceId = insertOccurrenceRecord(farmId, animalId, user.id(), "open", null);
        String accessToken = issueAccessToken(user, OffsetDateTime.now(ZoneOffset.UTC));
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        mockMvc.perform(post("/api/v1/medications")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "animalId": "%s",
                                          "occurrenceId": "%s",
                                          "medicationName": "Ivermectin",
                                          "activeIngredient": "Ivermectina",
                                          "reason": "Ticks",
                                          "dosage": "10 ml",
                                          "withdrawalPeriodDays": 35,
                                          "applicationDate": "%s",
                                          "cost": "45.90"
                                        }
                                        """.formatted(animalId, occurrenceId, today.minusDays(2))
                        ))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.animalId").value(animalId.toString()))
                .andExpect(jsonPath("$.occurrenceId").value(occurrenceId.toString()))
                .andExpect(jsonPath("$.medicationName").value("Ivermectin"))
                .andExpect(jsonPath("$.activeIngredient").value("Ivermectina"))
                .andExpect(jsonPath("$.reason").value("Ticks"))
                .andExpect(jsonPath("$.dosage").value("10 ml"))
                .andExpect(jsonPath("$.withdrawalPeriodDays").value(35))
                .andExpect(jsonPath("$.applicationDate").value(today.minusDays(2).toString()))
                .andExpect(jsonPath("$.releaseDate").value(today.plusDays(33).toString()))
                .andExpect(jsonPath("$.inWithdrawalPeriod").value(true))
                .andExpect(jsonPath("$.cost").value(45.90))
                .andExpect(jsonPath("$.source").value("app"))
                .andExpect(jsonPath("$.recordedBy").value(user.id().toString()));

        assertEquals(
                "Ivermectin",
                jdbcTemplate.queryForObject(
                        "select medication_name from medications where farm_id = ? and animal_id = ?",
                        String.class,
                        farmId,
                        animalId
                )
        );
    }

    @Test
    void invalidCreatePayloadReturnsFieldErrors() throws Exception {
        UUID farmId = insertFarm("Fazenda Medicacao Invalida");
        UserAccount admin = insertUser(farmId, "5511999999301", "Medicacao Admin", UserRole.ADMIN, true);
        String accessToken = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(post("/api/v1/medications")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "animalId": "not-a-uuid",
                                          "occurrenceId": "also-not-a-uuid",
                                          "medicationName": "",
                                          "activeIngredient": "%s",
                                          "dosage": "",
                                          "withdrawalPeriodDays": -1,
                                          "applicationDate": "2026-99-24",
                                          "cost": "-1.001"
                                        }
                                        """.formatted("x".repeat(201))
                        ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors.animalId").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.occurrenceId").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.medicationName").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.activeIngredient").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.dosage").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.withdrawalPeriodDays").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.applicationDate").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.cost").isNotEmpty());
    }

    @Test
    void returnsNotFoundWhenAnimalOrOccurrenceIsOutsideUsersFarm() throws Exception {
        UUID farmId = insertFarm("Fazenda Medicacao Um");
        UserAccount admin = insertUser(farmId, "5511999999302", "Medicacao Admin", UserRole.ADMIN, true);
        String accessToken = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));

        UUID otherFarmId = insertFarm("Fazenda Medicacao Dois");
        UUID foreignAnimalId = insertAnimal(otherFarmId, null, "MED-FOREIGN");
        UserAccount foreignUser = insertUser(otherFarmId, "5511999999303", "Medicacao Outra", UserRole.ADMIN, true);
        UUID foreignOccurrenceId = insertOccurrenceRecord(otherFarmId, foreignAnimalId, foreignUser.id(), "open", null);

        mockMvc.perform(post("/api/v1/medications")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "animalId": "%s",
                                          "medicationName": "Ivermectin",
                                          "dosage": "10 ml",
                                          "withdrawalPeriodDays": 35,
                                          "applicationDate": "2026-04-24"
                                        }
                                        """.formatted(foreignAnimalId)
                        ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not_found"));

        UUID localAnimalId = insertAnimal(farmId, null, "MED-LOCAL");

        mockMvc.perform(post("/api/v1/medications")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "animalId": "%s",
                                          "occurrenceId": "%s",
                                          "medicationName": "Ivermectin",
                                          "dosage": "10 ml",
                                          "withdrawalPeriodDays": 35,
                                          "applicationDate": "2026-04-24"
                                        }
                                        """.formatted(localAnimalId, foreignOccurrenceId)
                        ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not_found"));
    }

    @Test
    void workerCannotCreateOrListMedications() throws Exception {
        UUID farmId = insertFarm("Fazenda Worker Medicacao");
        UUID animalId = insertAnimal(farmId, null, "MED-W-001");
        UserAccount worker = insertUser(farmId, "5511999999304", "Medicacao Worker", UserRole.WORKER, true);
        String accessToken = issueAccessToken(worker, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(post("/api/v1/medications")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "animalId": "%s",
                                          "medicationName": "Ivermectin",
                                          "dosage": "10 ml",
                                          "withdrawalPeriodDays": 35,
                                          "applicationDate": "2026-04-24"
                                        }
                                        """.formatted(animalId)
                        ))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));

        mockMvc.perform(get("/api/v1/medications")
                        .param("animalId", animalId.toString())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
    }

    @Test
    void listsAnimalMedicationHistoryWithWithdrawalSemantics() throws Exception {
        UUID farmId = insertFarm("Fazenda Historico Medicacao");
        UUID animalId = insertAnimal(farmId, null, "MED-H-001");
        UserAccount veterinarian = insertUser(farmId, "5511999999305", "Medicacao Vet", UserRole.VETERINARIAN, true);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        insertHistoricalMedication(
                farmId,
                animalId,
                null,
                veterinarian.id(),
                "Ivermectin",
                "Ivermectina",
                "Ticks",
                "10 ml",
                today.minusDays(30),
                35,
                new BigDecimal("45.90")
        );
        insertHistoricalMedication(
                farmId,
                animalId,
                null,
                veterinarian.id(),
                "Anti-inflamatorio",
                null,
                "Dor",
                "5 ml",
                today.minusDays(90),
                3,
                new BigDecimal("12.50")
        );

        String accessToken = issueAccessToken(veterinarian, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/medications")
                        .param("animalId", animalId.toString())
                        .param("limit", "1")
                        .param("offset", "0")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.limit").value(1))
                .andExpect(jsonPath("$.offset").value(0))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].medicationName").value("Ivermectin"))
                .andExpect(jsonPath("$.items[0].releaseDate").value(today.plusDays(5).toString()))
                .andExpect(jsonPath("$.items[0].inWithdrawalPeriod").value(true));

        mockMvc.perform(get("/api/v1/medications")
                        .param("animalId", animalId.toString())
                        .param("limit", "2")
                        .param("offset", "0")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].medicationName").value("Ivermectin"))
                .andExpect(jsonPath("$.items[1].medicationName").value("Anti-inflamatorio"))
                .andExpect(jsonPath("$.items[1].inWithdrawalPeriod").value(false));
    }

    @Test
    void returnsNotFoundWhenListAnimalIsOutsideUsersFarm() throws Exception {
        UUID farmId = insertFarm("Fazenda Lista Medicacao");
        UserAccount veterinarian = insertUser(farmId, "5511999999306", "Lista Vet", UserRole.VETERINARIAN, true);
        String accessToken = issueAccessToken(veterinarian, OffsetDateTime.now(ZoneOffset.UTC));

        UUID otherFarmId = insertFarm("Outra Fazenda Lista Medicacao");
        UUID foreignAnimalId = insertAnimal(otherFarmId, null, "MED-LIST-FOREIGN");

        mockMvc.perform(get("/api/v1/medications")
                        .param("animalId", foreignAnimalId.toString())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not_found"));
    }

    @Test
    void invalidListQueryReturnsFieldErrors() throws Exception {
        UUID farmId = insertFarm("Fazenda Lista Medicacao Invalida");
        UserAccount admin = insertUser(farmId, "5511999999306", "Lista Admin", UserRole.ADMIN, true);
        String accessToken = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/medications")
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
    void authenticationIsRequiredForMedicationEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/medications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication_required"));
    }

    private void insertHistoricalMedication(
            UUID farmId,
            UUID animalId,
            UUID occurrenceId,
            UUID recordedBy,
            String medicationName,
            String activeIngredient,
            String reason,
            String dosage,
            LocalDate applicationDate,
            int withdrawalPeriodDays,
            BigDecimal cost
    ) {
        UUID medicationId = UUID.randomUUID();
        OffsetDateTime now = nowUtc();

        jdbcTemplate.update(
                """
                        insert into medications (
                            id, farm_id, animal_id, occurrence_id, medication_name, principio_ativo, motivo,
                            dosagem, data_aplicacao, periodo_withdrawal, release_date, custo, origem,
                            registrado_por, created_at, updated_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                medicationId,
                farmId,
                animalId,
                occurrenceId,
                medicationName,
                activeIngredient,
                reason,
                dosage,
                Date.valueOf(applicationDate),
                withdrawalPeriodDays,
                Date.valueOf(applicationDate.plusDays(withdrawalPeriodDays)),
                cost,
                "app",
                recordedBy,
                Timestamp.from(now.toInstant()),
                Timestamp.from(now.toInstant())
        );
    }

    private String phoneFor(UserRole role) {
        return switch (role) {
            case ADMIN -> "5511999999307";
            case VETERINARIAN -> "5511999999308";
            case WORKER -> "5511999999309";
        };
    }
}
