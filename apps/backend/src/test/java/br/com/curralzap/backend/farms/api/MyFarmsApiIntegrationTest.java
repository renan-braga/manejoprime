package br.com.curralzap.backend.farms.api;

import br.com.curralzap.backend.support.BackendApiIntegrationTestSupport;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserRole;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MyFarmsApiIntegrationTest extends BackendApiIntegrationTestSupport {

    // ── helpers ────────────────────────────────────────────────────────────────

    private String auth(UserAccount u) {
        return "Bearer " + issueAccessToken(u, OffsetDateTime.now(ZoneOffset.UTC));
    }

    private void postFarm(String token, String name, int expectedStatus) throws Exception {
        mockMvc.perform(post("/api/v1/my-farms")
                        .header("Authorization", token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"farmName\":\"" + name + "\"}"))
                .andExpect(status().is(expectedStatus));
    }

    private UserAccount ownerOf(UUID farmId, String phone) {
        UserAccount user = insertUser(farmId, phone, "Dono", UserRole.ADMIN, true);
        setFarmOwner(farmId, user.id());
        insertFarmMembership(user.id(), farmId, "admin");
        return user;
    }

    // ── trialing (no subscription, farm < 30 days) ────────────────────────────

    @Test
    void trialingAllowsUpToTwoOwnedFarms() throws Exception {
        UUID farmId = insertFarm("Fazenda Trial");
        UserAccount user = ownerOf(farmId, "5511900000001");
        String token = auth(user);

        postFarm(token, "Fazenda 2", 201);
        postFarm(token, "Fazenda 3", 402);
    }

    // ── pro/active: limit 2 ───────────────────────────────────────────────────

    @Test
    void proActiveAllowsUpToTwoOwnedFarms() throws Exception {
        UUID farmId = insertFarm("Fazenda Pro");
        insertBillingSubscription(farmId, "pro", "active", null, false);
        UserAccount user = ownerOf(farmId, "5511900000002");
        String token = auth(user);

        postFarm(token, "Fazenda 2", 201);
        postFarm(token, "Fazenda 3", 402);
    }

    @Test
    void proActiveBlocksWhenAlreadyAtLimit() throws Exception {
        UUID farm1 = insertFarm("Fazenda Pro 1");
        insertBillingSubscription(farm1, "pro", "active", null, false);
        UserAccount user = insertUser(farm1, "5511900000003", "Dono", UserRole.ADMIN, true);
        setFarmOwner(farm1, user.id());
        insertFarmMembership(user.id(), farm1, "admin");

        UUID farm2 = insertFarm("Fazenda Pro 2");
        setFarmOwner(farm2, user.id());
        insertFarmMembership(user.id(), farm2, "admin");

        postFarm(auth(user), "Fazenda 3", 402);
    }

    // ── suspended/canceled: revert to free (limit 1) ─────────────────────────

    @Test
    void suspendedProBlocksSecondOwnedFarm() throws Exception {
        UUID farmId = insertFarm("Fazenda Suspensa");
        insertBillingSubscription(farmId, "pro", "suspended", null, false);
        UserAccount user = ownerOf(farmId, "5511900000004");

        postFarm(auth(user), "Fazenda 2", 402);
    }

    @Test
    void canceledProBlocksSecondOwnedFarm() throws Exception {
        UUID farmId = insertFarm("Fazenda Cancelada");
        insertBillingSubscription(farmId, "pro", "canceled", null, false);
        UserAccount user = ownerOf(farmId, "5511900000005");

        postFarm(auth(user), "Fazenda 2", 402);
    }

    // ── business/active: limit 5 ──────────────────────────────────────────────

    @Test
    void businessActiveAllowsUpToFiveOwnedFarms() throws Exception {
        UUID farmId = insertFarm("Fazenda Business");
        insertBillingSubscription(farmId, "business", "active", null, false);
        UserAccount user = ownerOf(farmId, "5511900000006");
        String token = auth(user);

        for (int i = 2; i <= 5; i++) {
            postFarm(token, "Fazenda " + i, 201);
        }
        postFarm(token, "Fazenda 6", 402);
    }

    // ── shared farm does not leak higher plan to the user's own quota ─────────

    @Test
    void memberOfHighPlanFarmCannotBypassOwnTrialLimit() throws Exception {
        // Shared farm: business/active, owned by someone else
        UUID sharedFarmId = insertFarm("Fazenda Compartilhada");
        UUID otherId = UUID.randomUUID();
        jdbcTemplate.update("insert into users (id, farm_id, telefone, nome, role, ativo, created_at, updated_at) values (?, ?, ?, ?, ?, ?, now(), now())",
                otherId, sharedFarmId, "5511900000099", "Outro", "admin", true);
        setFarmOwner(sharedFarmId, otherId);
        insertFarmMembership(otherId, sharedFarmId, "admin");
        insertBillingSubscription(sharedFarmId, "business", "active", null, false);

        // User: owns 1 farm (no subscription = trialing, limit 2), is also member of shared farm
        UUID ownFarmId = insertFarm("Fazenda Propria");
        // JWT farmId = sharedFarmId (user is currently "on" the shared farm)
        UserAccount user = insertUser(sharedFarmId, "5511900000007", "Usuario", UserRole.ADMIN, true);
        setFarmOwner(ownFarmId, user.id());
        insertFarmMembership(user.id(), sharedFarmId, "admin");
        insertFarmMembership(user.id(), ownFarmId, "admin");
        String token = auth(user);

        // Can add 1 more owned farm (trialing limit = 2, owns 1)
        postFarm(token, "Fazenda Propria 2", 201);
        // Cannot add a 3rd (limit = 2, now owns 2) — business plan from shared farm must not apply
        postFarm(token, "Fazenda Propria 3", 402);
    }

    // ── error body ────────────────────────────────────────────────────────────

    @Test
    void quotaExceededResponseContainsLimit() throws Exception {
        UUID farmId = insertFarm("Fazenda Starter");
        insertBillingSubscription(farmId, "starter", "active", null, false);
        UserAccount user = ownerOf(farmId, "5511900000008");

        mockMvc.perform(post("/api/v1/my-farms")
                        .header("Authorization", auth(user))
                        .contentType(APPLICATION_JSON)
                        .content("{\"farmName\":\"Fazenda 2\"}"))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.code").value("farm_quota_exceeded"))
                .andExpect(jsonPath("$.details.limit").value(1));
    }
}
