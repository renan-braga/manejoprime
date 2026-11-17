package br.com.curralzap.backend.auth.api;

import br.com.curralzap.backend.support.BackendApiIntegrationTestSupport;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserRole;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SessionMeApiIntegrationTest extends BackendApiIntegrationTestSupport {

    @Test
    void returnsAuthenticatedSessionContext() throws Exception {
        UUID farmId = insertFarm("Fazenda Session");
        UserAccount userAccount = insertUser(farmId, "5511999990010", "Session Admin", UserRole.ADMIN, true);
        String accessToken = issueAccessToken(userAccount, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/session/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.userId").value(userAccount.id().toString()))
                .andExpect(jsonPath("$.name").value("Session Admin"))
                .andExpect(jsonPath("$.phone").value("5511999990010"))
                .andExpect(jsonPath("$.role").value("admin"))
                .andExpect(jsonPath("$.farmId").value(farmId.toString()))
                .andExpect(jsonPath("$.farmName").value("Fazenda Session"))
                .andExpect(jsonPath("$.permissions").isArray())
                .andExpect(jsonPath("$.permissions[?(@ == 'users.write')]").exists())
                .andExpect(jsonPath("$.billing.plan").value("trialing"))
                .andExpect(jsonPath("$.billing.status").value("trialing"))
                .andExpect(jsonPath("$.billing.billingExempt").value(false))
                .andExpect(jsonPath("$.billing.canWrite").value(true))
                .andExpect(jsonPath("$.billing.canUseWhatsapp").value(true));
    }

    @Test
    void returnsBillingContextWhenFarmHasSubscription() throws Exception {
        UUID farmId = insertFarm("Fazenda Billing Session");
        insertBillingSubscription(farmId, "pro", "suspended", null, true);
        UserAccount userAccount = insertUser(farmId, "5511999990012", "Billing Admin", UserRole.ADMIN, true);
        String accessToken = issueAccessToken(userAccount, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/session/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billing.plan").value("pro"))
                .andExpect(jsonPath("$.billing.status").value("suspended"))
                .andExpect(jsonPath("$.billing.billingExempt").value(true))
                .andExpect(jsonPath("$.billing.exemptionReason").value("test access"))
                .andExpect(jsonPath("$.billing.canWrite").value(true))
                .andExpect(jsonPath("$.billing.canUseWhatsapp").value(true));
    }

    @Test
    void returnsAuthenticationRequiredWhenHeaderIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/session/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value("authentication_required"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void secondFarmOfSameOwnerInheritsActiveBillingFromPrimaryFarm() throws Exception {
        UUID primaryFarmId = insertFarm("Fazenda Principal");
        insertBillingSubscription(primaryFarmId, "pro", "active", null, false);

        UUID secondaryFarmId = insertFarm("Fazenda Secundaria");

        UserAccount userAccount = insertUser(secondaryFarmId, "5511999990099", "Dono", UserRole.ADMIN, true);
        jdbcTemplate.update("update farms set owner_user_id = ? where id = ?", userAccount.id(), primaryFarmId);
        jdbcTemplate.update("update farms set owner_user_id = ? where id = ?", userAccount.id(), secondaryFarmId);

        String accessToken = issueAccessToken(userAccount, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/session/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.farmId").value(secondaryFarmId.toString()))
                .andExpect(jsonPath("$.billing.plan").value("pro"))
                .andExpect(jsonPath("$.billing.status").value("active"))
                .andExpect(jsonPath("$.billing.canWrite").value(true));
    }

    @Test
    void secondFarmOfSameOwnerInheritsTrialingFromPrimaryFarm() throws Exception {
        UUID primaryFarmId = insertFarm("Fazenda Trial");
        insertBillingSubscription(primaryFarmId, "pro", "trialing", null, false);

        UUID secondaryFarmId = insertFarm("Fazenda Nova");

        UserAccount userAccount = insertUser(secondaryFarmId, "5511999990098", "Dono Trial", UserRole.ADMIN, true);
        jdbcTemplate.update("update farms set owner_user_id = ? where id = ?", userAccount.id(), primaryFarmId);
        jdbcTemplate.update("update farms set owner_user_id = ? where id = ?", userAccount.id(), secondaryFarmId);

        String accessToken = issueAccessToken(userAccount, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/session/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.farmId").value(secondaryFarmId.toString()))
                .andExpect(jsonPath("$.billing.plan").value("pro"))
                .andExpect(jsonPath("$.billing.status").value("trialing"))
                .andExpect(jsonPath("$.billing.canWrite").value(true));
    }

    @Test
    void returnsAuthenticationFailedForMalformedToken() throws Exception {
        mockMvc.perform(get("/api/v1/session/me")
                        .header("Authorization", "Bearer malformed-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication_failed"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void returnsTokenExpiredForExpiredAccessToken() throws Exception {
        UUID farmId = insertFarm("Fazenda Expirada");
        UserAccount userAccount = insertUser(farmId, "5511999990011", "Expired Session", UserRole.WORKER, true);
        OffsetDateTime issuedAt = OffsetDateTime.now(ZoneOffset.UTC).minusHours(9);
        String accessToken = issueAccessToken(userAccount, issuedAt);

        mockMvc.perform(get("/api/v1/session/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("token_expired"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

}
