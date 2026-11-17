package br.com.curralzap.backend.farms.api;

import br.com.curralzap.backend.support.BackendApiIntegrationTestSupport;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserRole;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SettingsApiIntegrationTest extends BackendApiIntegrationTestSupport {

    @Test
    void adminCanReadFarmScopedSettings() throws Exception {
        UUID farmId = insertFarm("Fazenda Settings");
        UserAccount adminUser = insertUser(farmId, "5511999993800", "Settings Admin", UserRole.ADMIN, true);
        UUID otherFarmId = insertFarm("Outra Settings");
        updateFarmCarcassYield(otherFarmId, new BigDecimal("47.25"));

        String accessToken = issueAccessToken(adminUser, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/settings")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.farmId").value(farmId.toString()))
                .andExpect(jsonPath("$.farmName").value("Fazenda Settings"))
                .andExpect(jsonPath("$.carcassYieldPercent").value("50.00"));
    }

    @Test
    void adminCanUpdateFarmScopedSettings() throws Exception {
        UUID farmId = insertFarm("Fazenda Antiga");
        UserAccount adminUser = insertUser(farmId, "5511999993900", "Settings Updater", UserRole.ADMIN, true);
        UUID otherFarmId = insertFarm("Fazenda Intocada");
        updateFarmCarcassYield(otherFarmId, new BigDecimal("48.00"));
        String accessToken = issueAccessToken(adminUser, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(patch("/api/v1/settings")
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "farmName": "Fazenda Nova",
                                          "carcassYieldPercent": "52.75"
                                        }
                                        """
                        )
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.farmId").value(farmId.toString()))
                .andExpect(jsonPath("$.farmName").value("Fazenda Nova"))
                .andExpect(jsonPath("$.carcassYieldPercent").value("52.75"));

        String updatedName = jdbcTemplate.queryForObject(
                "select nome from farms where id = ?",
                String.class,
                farmId
        );
        BigDecimal updatedCarcassYield = jdbcTemplate.queryForObject(
                "select rendimento_carcaca from farms where id = ?",
                BigDecimal.class,
                farmId
        );
        BigDecimal untouchedCarcassYield = jdbcTemplate.queryForObject(
                "select rendimento_carcaca from farms where id = ?",
                BigDecimal.class,
                otherFarmId
        );

        org.junit.jupiter.api.Assertions.assertEquals("Fazenda Nova", updatedName);
        org.junit.jupiter.api.Assertions.assertEquals(0, new BigDecimal("52.75").compareTo(updatedCarcassYield));
        org.junit.jupiter.api.Assertions.assertEquals(0, new BigDecimal("48.00").compareTo(untouchedCarcassYield));
    }

    @Test
    void invalidSettingsPayloadReturnsFieldErrors() throws Exception {
        UUID farmId = insertFarm("Fazenda Settings Invalidos");
        UserAccount adminUser = insertUser(farmId, "5511999994000", "Settings Validator", UserRole.ADMIN, true);
        String accessToken = issueAccessToken(adminUser, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(patch("/api/v1/settings")
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "farmName": "",
                                          "carcassYieldPercent": "101.001"
                                        }
                                        """
                        )
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors.farmName").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.carcassYieldPercent").isNotEmpty());
    }

    @Test
    void nonAdminCannotReadOrUpdateSettings() throws Exception {
        UUID farmId = insertFarm("Fazenda Settings Worker");
        UserAccount workerUser = insertUser(farmId, "5511999994100", "Settings Worker", UserRole.WORKER, true);
        String accessToken = issueAccessToken(workerUser, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/settings")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));

        mockMvc.perform(patch("/api/v1/settings")
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "farmName": "Bloqueada",
                                          "carcassYieldPercent": "51.00"
                                        }
                                        """
                        )
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
    }

    @Test
    void requiresAuthenticationForSettingsEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/settings"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication_required"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    private void updateFarmCarcassYield(UUID farmId, BigDecimal carcassYield) {
        jdbcTemplate.update(
                "update farms set rendimento_carcaca = ? where id = ?",
                carcassYield,
                farmId
        );
    }
}
