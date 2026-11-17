package br.com.curralzap.backend.billing.api;

import br.com.curralzap.backend.support.BackendApiIntegrationTestSupport;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserRole;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BillingAccessFilterIntegrationTest extends BackendApiIntegrationTestSupport {

    @Test
    void suspendedFarmCanReadButCannotMutate() throws Exception {
        UUID farmId = insertFarm("Fazenda Suspensa");
        UUID animalId = insertAnimal(farmId, null, "BILL-001");
        UserAccount user = insertUser(farmId, "5511999997010", "Billing Admin", UserRole.ADMIN, true);
        insertBillingSubscription(farmId, "pro", "suspended", null, false);
        String accessToken = issueAccessToken(user, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/weighings")
                        .param("animalId", animalId.toString())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/weighings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "animalId": "%s",
                                  "weightKg": 500.00,
                                  "weighingDate": "2026-03-17",
                                  "type": "routine"
                                }
                                """.formatted(animalId)))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.code").value("payment_required"))
                .andExpect(jsonPath("$.details.plan").value("pro"))
                .andExpect(jsonPath("$.details.status").value("suspended"))
                .andExpect(jsonPath("$.details.canWrite").value(false));
    }

    @Test
    void billingExemptFarmCanMutateEvenWhenSuspended() throws Exception {
        UUID farmId = insertFarm("Fazenda Interna");
        UUID animalId = insertAnimal(farmId, null, "BILL-002");
        UserAccount user = insertUser(farmId, "5511999997020", "Internal Admin", UserRole.ADMIN, true);
        insertBillingSubscription(farmId, "internal", "suspended", null, true);
        String accessToken = issueAccessToken(user, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(post("/api/v1/weighings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "animalId": "%s",
                                  "weightKg": 501.00,
                                  "weighingDate": "2026-03-17",
                                  "type": "routine"
                                }
                                """.formatted(animalId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.animalId").value(animalId.toString()));
    }

    @Test
    void pastDueFarmCanMutateUntilGracePeriodEnds() throws Exception {
        UUID farmId = insertFarm("Fazenda Carencia");
        UUID animalId = insertAnimal(farmId, null, "BILL-003");
        UserAccount user = insertUser(farmId, "5511999997030", "Grace Admin", UserRole.ADMIN, true);
        insertBillingSubscription(
                farmId,
                "starter",
                "past_due",
                OffsetDateTime.now(ZoneOffset.UTC).plusDays(2),
                false
        );
        String accessToken = issueAccessToken(user, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(post("/api/v1/weighings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "animalId": "%s",
                                  "weightKg": 502.00,
                                  "weighingDate": "2026-03-17",
                                  "type": "routine"
                                }
                                """.formatted(animalId)))
                .andExpect(status().isOk());
    }
}
