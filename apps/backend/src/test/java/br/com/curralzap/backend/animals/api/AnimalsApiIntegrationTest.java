package br.com.curralzap.backend.animals.api;

import br.com.curralzap.backend.support.BackendApiIntegrationTestSupport;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserRole;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AnimalsApiIntegrationTest extends BackendApiIntegrationTestSupport {

    @Test
    void listsAnimalsWithFarmScopeFiltersAndPagination() throws Exception {
        UUID farmId = insertFarm("Fazenda Animais");
        UserAccount user = insertUser(farmId, "5511999991100", "Animal Reader", UserRole.ADMIN, true);
        UUID lotId = insertLot(farmId, "Engorda 1", "fattening");
        insertLot(farmId, "Cria", "raising");

        insertAnimal(farmId, lotId, "347", "Trovao", "Nelore", "macho", "ativo", LocalDate.now(ZoneOffset.UTC).minusDays(300), LocalDate.now(ZoneOffset.UTC).minusDays(120), new BigDecimal("31.20"));
        insertAnimal(farmId, lotId, "348", "Brisa", "Angus", "femea", "vendido", LocalDate.now(ZoneOffset.UTC).minusDays(410), LocalDate.now(ZoneOffset.UTC).minusDays(220), new BigDecimal("29.80"));
        insertAnimal(farmId, lotId, "349", "Trovao Novo", "Nelore", "macho", "ativo", LocalDate.now(ZoneOffset.UTC).minusDays(180), LocalDate.now(ZoneOffset.UTC).minusDays(90), new BigDecimal("33.10"));

        UUID otherFarmId = insertFarm("Outra Fazenda");
        insertUser(otherFarmId, "5511999991199", "Other Reader", UserRole.WORKER, true);
        insertAnimal(otherFarmId, null, "900", "Trovao Externo", "Nelore", "macho", "ativo", LocalDate.now(ZoneOffset.UTC).minusDays(200), LocalDate.now(ZoneOffset.UTC).minusDays(150), new BigDecimal("30.10"));

        String accessToken = issueAccessToken(user, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/animals")
                        .param("limit", "1")
                        .param("offset", "0")
                        .param("search", "trovao")
                        .param("status", "active")
                        .param("lot", lotId.toString())
                        .param("sexo", "male")
                        .param("raca", "nelo")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.limit").value(1))
                .andExpect(jsonPath("$.offset").value(0))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].identifier").value("347"))
                .andExpect(jsonPath("$.items[0].name").value("Trovao"))
                .andExpect(jsonPath("$.items[0].sex").value("male"))
                .andExpect(jsonPath("$.items[0].breed").value("Nelore"))
                .andExpect(jsonPath("$.items[0].status").value("active"))
                .andExpect(jsonPath("$.items[0].lotId").value(lotId.toString()))
                .andExpect(jsonPath("$.items[0].lotName").value("Engorda 1"));
    }

    @Test
    void listsAnimalsWithStableOffsetPagination() throws Exception {
        UUID farmId = insertFarm("Fazenda Ordem");
        UserAccount user = insertUser(farmId, "5511999991200", "Paginator", UserRole.ADMIN, true);
        insertAnimal(farmId, null, "001", "Alpha", "Nelore", "macho", "ativo", null, null, null);
        insertAnimal(farmId, null, "002", "Beta", "Nelore", "macho", "ativo", null, null, null);
        insertAnimal(farmId, null, "003", "Gamma", "Nelore", "macho", "ativo", null, null, null);

        String accessToken = issueAccessToken(user, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/animals")
                        .param("limit", "1")
                        .param("offset", "1")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].identifier").value("002"));
    }

    @Test
    void returnsAnimalDetailWithLotAndSummary() throws Exception {
        UUID farmId = insertFarm("Fazenda Detail");
        UserAccount user = insertUser(farmId, "5511999991300", "Detail Reader", UserRole.ADMIN, true);
        UUID lotId = insertLot(farmId, "Recria 2", "raising");
        LocalDate birthDate = LocalDate.now(ZoneOffset.UTC).minusDays(240);
        LocalDate entryDate = LocalDate.now(ZoneOffset.UTC).minusDays(180);
        UUID animalId = insertAnimal(farmId, lotId, "777", "Estrela", "Brahman", "femea", "ativo", birthDate, entryDate, new BigDecimal("34.50"));

        String accessToken = issueAccessToken(user, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/animals/{animalId}", animalId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.id").value(animalId.toString()))
                .andExpect(jsonPath("$.identifier").value("777"))
                .andExpect(jsonPath("$.name").value("Estrela"))
                .andExpect(jsonPath("$.sex").value("female"))
                .andExpect(jsonPath("$.breed").value("Brahman"))
                .andExpect(jsonPath("$.status").value("active"))
                .andExpect(jsonPath("$.birthDate").value(birthDate.toString()))
                .andExpect(jsonPath("$.entryDate").value(entryDate.toString()))
                .andExpect(jsonPath("$.farmId").value(farmId.toString()))
                .andExpect(jsonPath("$.lot.id").value(lotId.toString()))
                .andExpect(jsonPath("$.lot.name").value("Recria 2"))
                .andExpect(jsonPath("$.summary.birthWeightKg").value(34.50))
                .andExpect(jsonPath("$.summary.ageInDays").value(240))
                .andExpect(jsonPath("$.summary.daysOnFarm").value(180));
    }

    @Test
    void returnsNotFoundForAnimalOutsideTheAuthenticatedFarm() throws Exception {
        UUID farmId = insertFarm("Fazenda Um");
        UserAccount user = insertUser(farmId, "5511999991400", "Farm One", UserRole.ADMIN, true);
        UUID otherFarmId = insertFarm("Fazenda Dois");
        UUID foreignAnimalId = insertAnimal(otherFarmId, null, "901", "Alheio", "Nelore", "macho", "ativo", null, null, null);

        String accessToken = issueAccessToken(user, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/animals/{animalId}", foreignAnimalId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not_found"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void returnsValidationFailedForInvalidListFilters() throws Exception {
        UUID farmId = insertFarm("Fazenda Validacao");
        UserAccount user = insertUser(farmId, "5511999991500", "Validator", UserRole.ADMIN, true);
        String accessToken = issueAccessToken(user, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/animals")
                        .param("limit", "101")
                        .param("offset", "-1")
                        .param("lot", "invalid-uuid")
                        .param("status", "mystery")
                        .param("sexo", "unknown")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors.limit").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.offset").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.lot").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.status").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.sexo").isNotEmpty());
    }

    @Test
    void requiresAuthenticationForAnimalsEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/animals"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication_required"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void adminCanCreateAnimal() throws Exception {
        UUID farmId = insertFarm("Fazenda Create");
        UserAccount adminUser = insertUser(farmId, "5511999991600", "Create Admin", UserRole.ADMIN, true);
        String accessToken = issueAccessToken(adminUser, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(post("/api/v1/animals")
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "identifier": "350",
                                          "name": "Aurora",
                                          "breed": "Angus",
                                          "sex": "female",
                                          "birthDate": "2025-01-12",
                                          "birthWeightKg": "30.40",
                                          "status": "active",
                                          "entryDate": "2025-03-08"
                                        }
                                        """
                        )
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.id").isNotEmpty());

        Integer createdAnimals = jdbcTemplate.queryForObject(
                "select count(*) from animals where farm_id = ? and identificacao = ?",
                Integer.class,
                farmId,
                "350"
        );

        BigDecimal birthWeight = jdbcTemplate.queryForObject(
                "select peso_nascimento from animals where farm_id = ? and identificacao = ?",
                BigDecimal.class,
                farmId,
                "350"
        );

        String databaseStatus = jdbcTemplate.queryForObject(
                "select status from animals where farm_id = ? and identificacao = ?",
                String.class,
                farmId,
                "350"
        );

        assert createdAnimals != null;
        assert birthWeight != null;
        assert databaseStatus != null;
        org.junit.jupiter.api.Assertions.assertEquals(1, createdAnimals);
        org.junit.jupiter.api.Assertions.assertEquals(0, new BigDecimal("30.40").compareTo(birthWeight));
        org.junit.jupiter.api.Assertions.assertEquals("ativo", databaseStatus);
    }

    @Test
    void adminCanUpdateAnimal() throws Exception {
        UUID farmId = insertFarm("Fazenda Update");
        UserAccount adminUser = insertUser(farmId, "5511999991700", "Update Admin", UserRole.ADMIN, true);
        UUID animalId = insertAnimal(farmId, null, "351", "Ventania", "Nelore", "macho", "ativo", null, null, null);
        String accessToken = issueAccessToken(adminUser, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(patch("/api/v1/animals/{animalId}", animalId)
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "identifier": "351A",
                                          "name": "Ventania Nova",
                                          "breed": "Brahman",
                                          "sex": "female",
                                          "birthDate": "2025-02-10",
                                          "birthWeightKg": "31.15",
                                          "status": "sold",
                                          "entryDate": "2025-04-01"
                                        }
                                        """
                        )
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(animalId.toString()));

        String updatedIdentifier = jdbcTemplate.queryForObject(
                "select identificacao from animals where id = ?",
                String.class,
                animalId
        );
        String updatedSex = jdbcTemplate.queryForObject(
                "select sexo from animals where id = ?",
                String.class,
                animalId
        );
        String updatedStatus = jdbcTemplate.queryForObject(
                "select status from animals where id = ?",
                String.class,
                animalId
        );

        org.junit.jupiter.api.Assertions.assertEquals("351A", updatedIdentifier);
        org.junit.jupiter.api.Assertions.assertEquals("femea", updatedSex);
        org.junit.jupiter.api.Assertions.assertEquals("vendido", updatedStatus);
    }

    @Test
    void duplicateIdentifierReturnsValidationFailed() throws Exception {
        UUID farmId = insertFarm("Fazenda Duplicada");
        UserAccount adminUser = insertUser(farmId, "5511999991800", "Duplicate Admin", UserRole.ADMIN, true);
        insertAnimal(farmId, null, "352", "Primeiro", "Nelore", "macho", "ativo", null, null, null);
        String accessToken = issueAccessToken(adminUser, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(post("/api/v1/animals")
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "identifier": "352",
                                          "name": "Segundo",
                                          "breed": "Angus",
                                          "sex": "female",
                                          "birthDate": "2025-01-01",
                                          "birthWeightKg": "29.50",
                                          "status": "active",
                                          "entryDate": "2025-03-01"
                                        }
                                        """
                        )
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors.identifier").value("Identifier must be unique within the farm."));
    }

    @Test
    void invalidMutationPayloadReturnsFieldErrors() throws Exception {
        UUID farmId = insertFarm("Fazenda Invalid");
        UserAccount adminUser = insertUser(farmId, "5511999991900", "Invalid Admin", UserRole.ADMIN, true);
        String accessToken = issueAccessToken(adminUser, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(post("/api/v1/animals")
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "identifier": "",
                                          "name": "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
                                          "breed": "yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy",
                                          "sex": "unknown",
                                          "birthDate": "2025-14-01",
                                          "birthWeightKg": "-1.123",
                                          "status": "mystery",
                                          "entryDate": "not-a-date"
                                        }
                                        """
                        )
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors.identifier").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.name").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.breed").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.sex").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.birthDate").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.birthWeightKg").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.status").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.entryDate").isNotEmpty());
    }

    @Test
    void nonAdminCannotMutateAnimals() throws Exception {
        UUID farmId = insertFarm("Fazenda Worker");
        UserAccount workerUser = insertUser(farmId, "5511999991950", "Worker User", UserRole.WORKER, true);
        String accessToken = issueAccessToken(workerUser, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(post("/api/v1/animals")
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "identifier": "360",
                                          "name": "Bloqueado",
                                          "breed": "Nelore",
                                          "sex": "male",
                                          "birthDate": "2025-01-01",
                                          "birthWeightKg": "30.00",
                                          "status": "active",
                                          "entryDate": "2025-03-01"
                                        }
                                        """
                        )
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void adminCanDeleteAnimalWithNoRecords() throws Exception {
        UUID farmId = insertFarm("Fazenda Delete");
        UserAccount adminUser = insertUser(farmId, "5511999992000", "Delete Admin", UserRole.ADMIN, true);
        UUID animalId = insertAnimal(farmId, null, "999", "Descarte", "Nelore", "macho", "ativo", null, null, null);
        String accessToken = issueAccessToken(adminUser, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(delete("/api/v1/animals/{animalId}", animalId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        Integer remaining = jdbcTemplate.queryForObject(
                "select count(*) from animals where id = ?", Integer.class, animalId);
        org.junit.jupiter.api.Assertions.assertEquals(0, remaining);
    }

    @Test
    void deleteReturnsConflictWhenAnimalHasRecords() throws Exception {
        UUID farmId = insertFarm("Fazenda Delete Records");
        UserAccount adminUser = insertUser(farmId, "5511999992100", "Delete Records Admin", UserRole.ADMIN, true);
        UUID animalId = insertAnimal(farmId, null, "998", "ComPeso", "Nelore", "macho", "ativo", null, null, null);
        insertWeighing(farmId, animalId, adminUser.id(), new BigDecimal("430.00"), LocalDate.now(ZoneOffset.UTC));
        String accessToken = issueAccessToken(adminUser, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(delete("/api/v1/animals/{animalId}", animalId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("animal_has_records"));
    }

    @Test
    void deleteReturnsNotFoundForAnimalOutsideTheAuthenticatedFarm() throws Exception {
        UUID farmId = insertFarm("Fazenda Delete Um");
        UserAccount adminUser = insertUser(farmId, "5511999992200", "Delete Admin Farm", UserRole.ADMIN, true);
        UUID otherFarmId = insertFarm("Fazenda Delete Dois");
        UUID foreignAnimalId = insertAnimal(otherFarmId, null, "997", "Alheio", "Nelore", "macho", "ativo", null, null, null);
        String accessToken = issueAccessToken(adminUser, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(delete("/api/v1/animals/{animalId}", foreignAnimalId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not_found"));
    }

    @Test
    void updateReturnsNotFoundForAnimalOutsideTheAuthenticatedFarm() throws Exception {
        UUID farmId = insertFarm("Fazenda Patch Um");
        UserAccount adminUser = insertUser(farmId, "5511999991960", "Patch Admin", UserRole.ADMIN, true);
        UUID otherFarmId = insertFarm("Fazenda Patch Dois");
        UUID foreignAnimalId = insertAnimal(otherFarmId, null, "902", "Alheio", "Nelore", "macho", "ativo", null, null, null);
        String accessToken = issueAccessToken(adminUser, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(patch("/api/v1/animals/{animalId}", foreignAnimalId)
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "identifier": "902A",
                                          "name": "Nao Pode",
                                          "breed": "Nelore",
                                          "sex": "male",
                                          "birthDate": "2025-01-01",
                                          "birthWeightKg": "30.10",
                                          "status": "active",
                                          "entryDate": "2025-03-01"
                                        }
                                        """
                        )
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not_found"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

}
