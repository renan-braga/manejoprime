package br.com.curralzap.backend.vaccines.api;

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

class VaccinesApiIntegrationTest extends BackendApiIntegrationTestSupport {

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"ADMIN", "VETERINARIAN"})
    void adminAndVeterinarianCanCreateAnimalVaccine(UserRole role) throws Exception {
        UUID farmId = insertFarm("Fazenda Vacinas Animal");
        UUID animalId = insertAnimal(farmId, null, "VAC-A-001");
        UserAccount user = insertUser(farmId, phoneFor(role), "Vacina " + role.name(), role, true);
        String accessToken = issueAccessToken(user, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(post("/api/v1/vaccines")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "animalId": "%s",
                                          "vaccineType": "Aftosa",
                                          "vaccineBatch": "AF-123",
                                          "dose": "primeira",
                                          "applicationDate": "2026-04-24",
                                          "nextDoseDate": "2026-05-24",
                                          "volumeMl": "2.50"
                                        }
                                        """.formatted(animalId)
                        ))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.animalId").value(animalId.toString()))
                .andExpect(jsonPath("$.lotId").doesNotExist())
                .andExpect(jsonPath("$.vaccineType").value("Aftosa"))
                .andExpect(jsonPath("$.vaccineBatch").value("AF-123"))
                .andExpect(jsonPath("$.dose").value("primeira"))
                .andExpect(jsonPath("$.applicationDate").value("2026-04-24"))
                .andExpect(jsonPath("$.nextDoseDate").value("2026-05-24"))
                .andExpect(jsonPath("$.volumeMl").value(2.50))
                .andExpect(jsonPath("$.source").value("app"))
                .andExpect(jsonPath("$.recordedBy").value(user.id().toString()));

        String persistedDose = jdbcTemplate.queryForObject(
                "select dose from vaccines where farm_id = ? and animal_id = ?",
                String.class,
                farmId,
                animalId
        );

        assertEquals("primeira", persistedDose);
    }

    @Test
    void adminCanCreateLotVaccine() throws Exception {
        UUID farmId = insertFarm("Fazenda Vacinas Lote");
        UUID lotId = insertLot(farmId, "Recria", "raising");
        UserAccount admin = insertUser(farmId, "5511999999101", "Lote Admin", UserRole.ADMIN, true);
        String accessToken = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(post("/api/v1/vaccines")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "lotId": "%s",
                                          "vaccineType": "Raiva",
                                          "dose": "reforco",
                                          "applicationDate": "2026-04-20",
                                          "volumeMl": "5.00"
                                        }
                                        """.formatted(lotId)
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.animalId").doesNotExist())
                .andExpect(jsonPath("$.lotId").value(lotId.toString()))
                .andExpect(jsonPath("$.dose").value("reforco"));
    }

    @Test
    void invalidCreatePayloadReturnsFieldErrors() throws Exception {
        UUID farmId = insertFarm("Fazenda Vacina Invalida");
        UserAccount admin = insertUser(farmId, "5511999999102", "Vacina Admin", UserRole.ADMIN, true);
        String accessToken = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(post("/api/v1/vaccines")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "animalId": "not-a-uuid",
                                          "lotId": "also-not-a-uuid",
                                          "vaccineType": "",
                                          "vaccineBatch": "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
                                          "dose": "terceira",
                                          "applicationDate": "2026-99-24",
                                          "nextDoseDate": "2026-99-01",
                                          "volumeMl": "-1.001"
                                        }
                                        """
                        ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors.target").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.vaccineType").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.vaccineBatch").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.dose").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.applicationDate").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.nextDoseDate").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.volumeMl").isNotEmpty());
    }

    @Test
    void returnsNotFoundWhenCreateTargetIsOutsideUsersFarm() throws Exception {
        UUID farmId = insertFarm("Fazenda Vacina");
        UserAccount admin = insertUser(farmId, "5511999999103", "Vacina Admin", UserRole.ADMIN, true);
        UUID otherFarmId = insertFarm("Outra Fazenda");
        UUID foreignAnimalId = insertAnimal(otherFarmId, null, "VAC-FOREIGN");
        String accessToken = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(post("/api/v1/vaccines")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "animalId": "%s",
                                          "vaccineType": "Brucelose",
                                          "dose": "anual",
                                          "applicationDate": "2026-04-24"
                                        }
                                        """.formatted(foreignAnimalId)
                        ))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not_found"));
    }

    @Test
    void workerCannotCreateOrListVaccines() throws Exception {
        UUID farmId = insertFarm("Fazenda Worker Vacina");
        UUID animalId = insertAnimal(farmId, null, "VAC-W-001");
        UserAccount worker = insertUser(farmId, "5511999999104", "Vacina Worker", UserRole.WORKER, true);
        String accessToken = issueAccessToken(worker, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(post("/api/v1/vaccines")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "animalId": "%s",
                                          "vaccineType": "Aftosa",
                                          "dose": "primeira",
                                          "applicationDate": "2026-04-24"
                                        }
                                        """.formatted(animalId)
                        ))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));

        mockMvc.perform(get("/api/v1/vaccines")
                        .param("animalId", animalId.toString())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
    }

    @Test
    void listsAnimalVaccinesWithStableOffsetPagination() throws Exception {
        UUID farmId = insertFarm("Fazenda Historico Animal");
        UUID animalId = insertAnimal(farmId, null, "VAC-H-001");
        UserAccount veterinarian = insertUser(farmId, "5511999999105", "Vacina Vet", UserRole.VETERINARIAN, true);
        insertHistoricalVaccine(farmId, animalId, null, veterinarian.id(), "Aftosa", "primeira", LocalDate.of(2026, 4, 1), LocalDate.of(2026, 5, 1));
        insertHistoricalVaccine(farmId, animalId, null, veterinarian.id(), "Brucelose", "segunda", LocalDate.of(2026, 4, 15), null);
        insertHistoricalVaccine(farmId, animalId, null, veterinarian.id(), "Raiva", "anual", LocalDate.of(2026, 4, 20), LocalDate.of(2027, 4, 20));

        String accessToken = issueAccessToken(veterinarian, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/vaccines")
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
                .andExpect(jsonPath("$.items[0].vaccineType").value("Raiva"))
                .andExpect(jsonPath("$.items[0].applicationDate").value("2026-04-20"))
                .andExpect(jsonPath("$.items[1].vaccineType").value("Brucelose"));
    }

    @Test
    void listsLotVaccines() throws Exception {
        UUID farmId = insertFarm("Fazenda Historico Lote");
        UUID lotId = insertLot(farmId, "Engorda", "fattening");
        UserAccount admin = insertUser(farmId, "5511999999106", "Vacina Admin", UserRole.ADMIN, true);
        insertHistoricalVaccine(farmId, null, lotId, admin.id(), "Raiva", "reforco", LocalDate.of(2026, 4, 10), null);
        insertHistoricalVaccine(farmId, null, lotId, admin.id(), "Aftosa", "primeira", LocalDate.of(2026, 4, 2), LocalDate.of(2026, 5, 2));

        String accessToken = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/vaccines")
                        .param("lotId", lotId.toString())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items[0].lotId").value(lotId.toString()))
                .andExpect(jsonPath("$.items[0].vaccineType").value("Raiva"))
                .andExpect(jsonPath("$.items[1].vaccineType").value("Aftosa"));
    }

    @Test
    void returnsNotFoundWhenListTargetIsOutsideUsersFarm() throws Exception {
        UUID farmId = insertFarm("Fazenda Lista Vacina");
        UserAccount veterinarian = insertUser(farmId, "5511999999108", "Lista Vet", UserRole.VETERINARIAN, true);
        String accessToken = issueAccessToken(veterinarian, OffsetDateTime.now(ZoneOffset.UTC));

        UUID otherFarmId = insertFarm("Outra Fazenda Lista Vacina");
        UUID foreignAnimalId = insertAnimal(otherFarmId, null, "VAC-LIST-FOREIGN");
        UUID foreignLotId = insertLot(otherFarmId, "Outro Lote", "raising");

        mockMvc.perform(get("/api/v1/vaccines")
                        .param("animalId", foreignAnimalId.toString())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not_found"));

        mockMvc.perform(get("/api/v1/vaccines")
                        .param("lotId", foreignLotId.toString())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not_found"));
    }

    @Test
    void invalidListQueryReturnsFieldErrors() throws Exception {
        UUID farmId = insertFarm("Fazenda Lista Invalida");
        UserAccount admin = insertUser(farmId, "5511999999107", "Lista Admin", UserRole.ADMIN, true);
        String accessToken = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/vaccines")
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
    void authenticationIsRequiredForVaccineEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/vaccines"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication_required"));
    }

    private String phoneFor(UserRole role) {
        return switch (role) {
            case ADMIN -> "5511999999108";
            case VETERINARIAN -> "5511999999109";
            case WORKER -> "5511999999110";
        };
    }

    private void insertHistoricalVaccine(
            UUID farmId,
            UUID animalId,
            UUID lotId,
            UUID recordedBy,
            String vaccineType,
            String dose,
            LocalDate applicationDate,
            LocalDate nextDoseDate
    ) {
        UUID vaccineId = UUID.randomUUID();
        OffsetDateTime now = nowUtc();

        jdbcTemplate.update(
                """
                        insert into vaccines (
                            id, farm_id, animal_id, lot_id, vaccine_type, vaccine_batch, dose, data_aplicacao,
                            data_proxima_dose, volume_ml, origem, registrado_por, created_at, updated_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                vaccineId,
                farmId,
                animalId,
                lotId,
                vaccineType,
                vaccineType.substring(0, Math.min(vaccineType.length(), 3)).toUpperCase() + "-001",
                dose,
                Date.valueOf(applicationDate),
                nextDoseDate != null ? Date.valueOf(nextDoseDate) : null,
                new BigDecimal("2.50"),
                "app",
                recordedBy,
                Timestamp.from(now.toInstant()),
                Timestamp.from(now.toInstant())
        );
    }
}
