package br.com.curralzap.backend.lots.api;

import br.com.curralzap.backend.support.BackendApiIntegrationTestSupport;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserRole;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LotsApiIntegrationTest extends BackendApiIntegrationTestSupport {

    @Test
    void listsLotsWithFarmScopeFiltersAndAnimalsCount() throws Exception {
        UUID farmId = insertFarm("Fazenda Lotes");
        UserAccount workerUser = insertUser(farmId, "5511999992100", "Lot Worker", UserRole.WORKER, true);
        UUID lotId = insertLot(farmId, "Engorda 1", "fattening", "Pasto Norte");
        insertLot(farmId, "Cria 2", "raising", "Pasto Sul");
        insertAnimal(farmId, lotId, "A-001");
        insertAnimal(farmId, lotId, "A-002");

        UUID otherFarmId = insertFarm("Outra Fazenda");
        insertUser(otherFarmId, "5511999992101", "Other Worker", UserRole.WORKER, true);
        UUID foreignLotId = insertLot(otherFarmId, "Engorda Externa", "fattening", "Pasto Externo");
        insertAnimal(otherFarmId, foreignLotId, "B-001");

        String accessToken = issueAccessToken(workerUser, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/lots")
                        .param("search", "engorda")
                        .param("purpose", "fattening")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(lotId.toString()))
                .andExpect(jsonPath("$.items[0].name").value("Engorda 1"))
                .andExpect(jsonPath("$.items[0].purpose").value("fattening"))
                .andExpect(jsonPath("$.items[0].pastureArea").value("Pasto Norte"))
                .andExpect(jsonPath("$.items[0].animalsCount").value(2));
    }

    @Test
    void listsLotsWithStableOffsetPagination() throws Exception {
        UUID farmId = insertFarm("Fazenda Paginacao");
        UserAccount adminUser = insertUser(farmId, "5511999992200", "Lot Admin", UserRole.ADMIN, true);
        insertLot(farmId, "Alpha", "breeding", null);
        insertLot(farmId, "Beta", "raising", null);
        insertLot(farmId, "Gamma", "fattening", null);

        String accessToken = issueAccessToken(adminUser, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/lots")
                        .param("limit", "1")
                        .param("offset", "1")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.limit").value(1))
                .andExpect(jsonPath("$.offset").value(1))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Beta"));
    }

    @Test
    void returnsLotDetailWithAnimalsCount() throws Exception {
        UUID farmId = insertFarm("Fazenda Detail");
        UserAccount workerUser = insertUser(farmId, "5511999992300", "Lot Detail User", UserRole.WORKER, true);
        UUID lotId = insertLot(farmId, "Recria 4", "raising", "Pasto Central");
        insertAnimal(farmId, lotId, "C-001");
        insertAnimal(farmId, lotId, "C-002");
        insertAnimal(farmId, lotId, "C-003");

        String accessToken = issueAccessToken(workerUser, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/lots/{lotId}", lotId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.id").value(lotId.toString()))
                .andExpect(jsonPath("$.farmId").value(farmId.toString()))
                .andExpect(jsonPath("$.name").value("Recria 4"))
                .andExpect(jsonPath("$.purpose").value("raising"))
                .andExpect(jsonPath("$.pastureArea").value("Pasto Central"))
                .andExpect(jsonPath("$.animalsCount").value(3));
    }

    @Test
    void returnsValidationFailedForInvalidLotListFilters() throws Exception {
        UUID farmId = insertFarm("Fazenda Validacao");
        UserAccount adminUser = insertUser(farmId, "5511999992400", "Lot Validator", UserRole.ADMIN, true);
        String accessToken = issueAccessToken(adminUser, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/lots")
                        .param("limit", "101")
                        .param("offset", "-1")
                        .param("purpose", "unknown")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors.limit").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.offset").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.purpose").isNotEmpty());
    }

    @Test
    void requiresAuthenticationForLotsEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/lots"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication_required"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void adminCanCreateLot() throws Exception {
        UUID farmId = insertFarm("Fazenda Create");
        UserAccount adminUser = insertUser(farmId, "5511999992500", "Create Lot Admin", UserRole.ADMIN, true);
        String accessToken = issueAccessToken(adminUser, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(post("/api/v1/lots")
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "name": "Engorda Nova",
                                          "purpose": "fattening",
                                          "pastureArea": "Pasto 12"
                                        }
                                        """
                        )
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.farmId").value(farmId.toString()))
                .andExpect(jsonPath("$.name").value("Engorda Nova"))
                .andExpect(jsonPath("$.purpose").value("fattening"))
                .andExpect(jsonPath("$.pastureArea").value("Pasto 12"))
                .andExpect(jsonPath("$.animalsCount").value(0));

        Integer createdLots = jdbcTemplate.queryForObject(
                "select count(*) from lots where farm_id = ? and nome = ?",
                Integer.class,
                farmId,
                "Engorda Nova"
        );
        String databasePurpose = jdbcTemplate.queryForObject(
                "select finalidade from lots where farm_id = ? and nome = ?",
                String.class,
                farmId,
                "Engorda Nova"
        );
        String databasePastureArea = jdbcTemplate.queryForObject(
                "select pasto_area from lots where farm_id = ? and nome = ?",
                String.class,
                farmId,
                "Engorda Nova"
        );

        org.junit.jupiter.api.Assertions.assertEquals(1, createdLots);
        org.junit.jupiter.api.Assertions.assertEquals("fattening", databasePurpose);
        org.junit.jupiter.api.Assertions.assertEquals("Pasto 12", databasePastureArea);
    }

    @Test
    void adminCanUpdateLot() throws Exception {
        UUID farmId = insertFarm("Fazenda Update");
        UserAccount adminUser = insertUser(farmId, "5511999992600", "Update Lot Admin", UserRole.ADMIN, true);
        UUID lotId = insertLot(farmId, "Lote Antigo", "breeding", "Pasto A");
        String accessToken = issueAccessToken(adminUser, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(patch("/api/v1/lots/{lotId}", lotId)
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "name": "Lote Atualizado",
                                          "purpose": "reproduction",
                                          "pastureArea": "Pasto B"
                                        }
                                        """
                        )
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(lotId.toString()))
                .andExpect(jsonPath("$.name").value("Lote Atualizado"))
                .andExpect(jsonPath("$.purpose").value("reproduction"))
                .andExpect(jsonPath("$.pastureArea").value("Pasto B"));

        String updatedName = jdbcTemplate.queryForObject(
                "select nome from lots where id = ?",
                String.class,
                lotId
        );
        String updatedPurpose = jdbcTemplate.queryForObject(
                "select finalidade from lots where id = ?",
                String.class,
                lotId
        );

        org.junit.jupiter.api.Assertions.assertEquals("Lote Atualizado", updatedName);
        org.junit.jupiter.api.Assertions.assertEquals("reproduction", updatedPurpose);
    }

    @Test
    void invalidLotPayloadReturnsFieldErrors() throws Exception {
        UUID farmId = insertFarm("Fazenda Invalid");
        UserAccount adminUser = insertUser(farmId, "5511999992700", "Invalid Lot Admin", UserRole.ADMIN, true);
        String accessToken = issueAccessToken(adminUser, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(post("/api/v1/lots")
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "name": "",
                                          "purpose": "mystery",
                                          "pastureArea": "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
                                        }
                                        """
                        )
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors.name").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.purpose").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.pastureArea").isNotEmpty());
    }

    @Test
    void nonAdminCannotMutateLots() throws Exception {
        UUID farmId = insertFarm("Fazenda Worker");
        UserAccount workerUser = insertUser(farmId, "5511999992800", "Worker Lot User", UserRole.WORKER, true);
        String accessToken = issueAccessToken(workerUser, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(post("/api/v1/lots")
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "name": "Lote Bloqueado",
                                          "purpose": "raising",
                                          "pastureArea": "Pasto X"
                                        }
                                        """
                        )
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void updateReturnsNotFoundForLotOutsideTheAuthenticatedFarm() throws Exception {
        UUID farmId = insertFarm("Fazenda Patch Um");
        UserAccount adminUser = insertUser(farmId, "5511999992900", "Patch Lot Admin", UserRole.ADMIN, true);
        UUID otherFarmId = insertFarm("Fazenda Patch Dois");
        UUID foreignLotId = insertLot(otherFarmId, "Lote Alheio", "raising", "Pasto Distante");
        String accessToken = issueAccessToken(adminUser, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(patch("/api/v1/lots/{lotId}", foreignLotId)
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "name": "Nao Pode",
                                          "purpose": "fattening",
                                          "pastureArea": "Pasto Novo"
                                        }
                                        """
                        )
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not_found"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

}
