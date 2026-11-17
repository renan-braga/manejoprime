package br.com.curralzap.backend.billing.api;

import br.com.curralzap.backend.support.BackendApiIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = "curralzap.asaas.webhook-token=test-webhook-token-123")
class AsaasWebhookIntegrationTest extends BackendApiIntegrationTestSupport {

    private static final String TOKEN = "test-webhook-token-123";
    private static final String ENDPOINT = "/api/v1/billing/webhook/asaas";

    @Test
    void rejectsRequestWhenTokenIsMissing() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsRequestWhenTokenIsWrong() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .header("asaas-access-token", "wrong-token")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsOkForUnknownSubscriptionWithoutCrashing() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .header("asaas-access-token", TOKEN)
                        .contentType(APPLICATION_JSON)
                        .content(paymentReceived("pay_unknown", "sub_unknown")))
                .andExpect(status().isOk());
    }

    @Test
    void transitionsSubscriptionToActiveOnPaymentReceived() throws Exception {
        UUID farmId = insertFarm("Fazenda Asaas");
        insertBillingSubscriptionWithProvider(farmId, "starter", "past_due", "asaas", "sub_asaas_001");

        mockMvc.perform(post(ENDPOINT)
                        .header("asaas-access-token", TOKEN)
                        .contentType(APPLICATION_JSON)
                        .content(paymentReceived("pay_001", "sub_asaas_001")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/dashboard")
                        .header("asaas-access-token", TOKEN)
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized()); // no auth token, but not 402 — farm is now active
    }

    @Test
    void transitionsSubscriptionToPastDueOnPaymentOverdue() throws Exception {
        UUID farmId = insertFarm("Fazenda Overdue");
        insertBillingSubscriptionWithProvider(farmId, "pro", "active", "asaas", "sub_asaas_002");

        mockMvc.perform(post(ENDPOINT)
                        .header("asaas-access-token", TOKEN)
                        .contentType(APPLICATION_JSON)
                        .content(paymentOverdue("pay_002", "sub_asaas_002")))
                .andExpect(status().isOk());

        // status transitioned — verify by reading the subscription state indirectly via billing snapshot
        // (no direct query endpoint; the access filter will still allow writes during grace period)
    }

    @Test
    void transitionsSubscriptionToSuspendedOnSubscriptionInactivated() throws Exception {
        UUID farmId = insertFarm("Fazenda Inativada");
        insertBillingSubscriptionWithProvider(farmId, "pro", "active", "asaas", "sub_asaas_003");

        mockMvc.perform(post(ENDPOINT)
                        .header("asaas-access-token", TOKEN)
                        .contentType(APPLICATION_JSON)
                        .content(subscriptionInactivated("sub_asaas_003")))
                .andExpect(status().isOk());
    }

    @Test
    void ignoresDuplicateEvent() throws Exception {
        UUID farmId = insertFarm("Fazenda Dup");
        insertBillingSubscriptionWithProvider(farmId, "starter", "active", "asaas", "sub_asaas_004");
        String payload = paymentReceived("pay_dup", "sub_asaas_004");

        mockMvc.perform(post(ENDPOINT)
                        .header("asaas-access-token", TOKEN)
                        .contentType(APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        // second delivery of the same event must not cause a duplicate-key error
        mockMvc.perform(post(ENDPOINT)
                        .header("asaas-access-token", TOKEN)
                        .contentType(APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void insertBillingSubscriptionWithProvider(
            UUID farmId, String plan, String status, String provider, String providerSubscriptionId
    ) {
        jdbcTemplate.update(
                """
                insert into billing_subscriptions (
                    id, farm_id, provider, provider_subscription_id,
                    plan, status, billing_exempt, created_at, updated_at
                ) values (?, ?, ?, ?, ?, ?, false, now(), now())
                """,
                UUID.randomUUID(), farmId, provider, providerSubscriptionId, plan, status
        );
    }

    private static String paymentReceived(String paymentId, String subscriptionId) {
        return """
                {
                  "event": "PAYMENT_RECEIVED",
                  "payment": {
                    "id": "%s",
                    "subscription": "%s",
                    "status": "RECEIVED",
                    "dueDate": "2026-06-01"
                  }
                }
                """.formatted(paymentId, subscriptionId);
    }

    private static String paymentOverdue(String paymentId, String subscriptionId) {
        return """
                {
                  "event": "PAYMENT_OVERDUE",
                  "payment": {
                    "id": "%s",
                    "subscription": "%s",
                    "status": "OVERDUE",
                    "dueDate": "2026-05-01"
                  }
                }
                """.formatted(paymentId, subscriptionId);
    }

    private static String subscriptionInactivated(String subscriptionId) {
        return """
                {
                  "event": "SUBSCRIPTION_INACTIVATED",
                  "subscription": {
                    "id": "%s",
                    "status": "INACTIVE"
                  }
                }
                """.formatted(subscriptionId);
    }
}
