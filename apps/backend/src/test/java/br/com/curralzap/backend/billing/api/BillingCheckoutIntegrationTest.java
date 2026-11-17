package br.com.curralzap.backend.billing.api;

import br.com.curralzap.backend.billing.application.AsaasGateway;
import br.com.curralzap.backend.billing.application.AsaasGateway.SubscriptionResult;
import br.com.curralzap.backend.billing.application.BillingGatewayException;
import br.com.curralzap.backend.billing.application.BillingSubscriptionRepository;
import br.com.curralzap.backend.support.BackendApiIntegrationTestSupport;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BillingCheckoutIntegrationTest extends BackendApiIntegrationTestSupport {

    @MockitoBean
    AsaasGateway asaasGateway;

    @MockitoBean
    BillingSubscriptionRepository billingSubscriptionRepository;

    private static final String ENDPOINT = "/api/v1/billing/checkout";

    @Test
    void returnsPaymentDataForBoletoCheckout() throws Exception {
        UUID farmId = insertFarm("Fazenda Boleto");
        UserAccount user = insertUser(farmId, "5511900000001", "Admin Boleto", UserRole.ADMIN, true);
        String token = issueAccessToken(user, OffsetDateTime.now(ZoneOffset.UTC));

        when(asaasGateway.createOrFindCustomer(anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("cus_boleto_001");

        when(asaasGateway.createSubscription(
                eq("cus_boleto_001"), eq("BOLETO"), any(), any(), anyString(), eq("MONTHLY"), any(), any()))
                .thenReturn(SubscriptionResult.boleto(
                        "sub_boleto_001",
                        "https://sandbox.asaas.com/i/boleto001",
                        "https://sandbox.asaas.com/b/pdf/boleto001",
                        "34191.75600 12345.678906 79999.990000 1 00000000089900"
                ));

        mockMvc.perform(post(ENDPOINT)
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(checkoutBody("starter", "boleto", "monthly")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentMethod", is("boleto")))
                .andExpect(jsonPath("$.paymentUrl", is("https://sandbox.asaas.com/i/boleto001")))
                .andExpect(jsonPath("$.bankSlipUrl", is("https://sandbox.asaas.com/b/pdf/boleto001")))
                .andExpect(jsonPath("$.boletoCode", is("34191.75600 12345.678906 79999.990000 1 00000000089900")));
    }

    @Test
    void returnsPaymentDataForPixCheckout() throws Exception {
        UUID farmId = insertFarm("Fazenda PIX");
        UserAccount user = insertUser(farmId, "5511900000007", "Admin PIX", UserRole.ADMIN, true);
        String token = issueAccessToken(user, OffsetDateTime.now(ZoneOffset.UTC));

        when(asaasGateway.createOrFindCustomer(anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("cus_pix_001");

        when(asaasGateway.createSubscription(
                eq("cus_pix_001"), eq("PIX"), any(), any(), anyString(), eq("MONTHLY"), any(), any()))
                .thenReturn(SubscriptionResult.pix(
                        "sub_pix_001",
                        "https://sandbox.asaas.com/i/pix001",
                        "data:image/png;base64,iVBORw0KGgoAAAANS",
                        "00020126580014br.gov.bcb.pix0136abc123"
                ));

        mockMvc.perform(post(ENDPOINT)
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(checkoutBody("starter", "pix", "monthly")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentMethod", is("pix")))
                .andExpect(jsonPath("$.paymentUrl", is("https://sandbox.asaas.com/i/pix001")))
                .andExpect(jsonPath("$.pixQrCodeImage", startsWith("data:image/png;base64,")))
                .andExpect(jsonPath("$.pixCopyPaste", startsWith("00020126")));
    }

    @Test
    void returnsPaymentDataForCardCheckoutWithDirectCharge() throws Exception {
        UUID farmId = insertFarm("Fazenda Cartão");
        UserAccount user = insertUser(farmId, "5511900000002", "Admin Cartao", UserRole.ADMIN, true);
        String token = issueAccessToken(user, OffsetDateTime.now(ZoneOffset.UTC));

        when(asaasGateway.createOrFindCustomer(anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("cus_card_001");

        when(asaasGateway.createSubscription(
                eq("cus_card_001"), eq("CREDIT_CARD"), any(), any(), anyString(), eq("MONTHLY"), any(), any()))
                .thenReturn(SubscriptionResult.cardCharged("sub_card_001"));

        mockMvc.perform(post(ENDPOINT)
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(checkoutBodyWithCard("pro", "card", "monthly")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentMethod", is("card")))
                .andExpect(jsonPath("$.paymentUrl", nullValue()));
    }

    @Test
    void returnsPaymentDataForAnnualBoletoCheckout() throws Exception {
        UUID farmId = insertFarm("Fazenda Anual");
        UserAccount user = insertUser(farmId, "5511900000003", "Admin Anual", UserRole.ADMIN, true);
        String token = issueAccessToken(user, OffsetDateTime.now(ZoneOffset.UTC));

        when(asaasGateway.createOrFindCustomer(anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("cus_annual_001");

        when(asaasGateway.createSubscription(
                eq("cus_annual_001"), eq("BOLETO"), any(), any(), anyString(), eq("YEARLY"), any(), any()))
                .thenReturn(SubscriptionResult.boleto(
                        "sub_annual_001",
                        "https://sandbox.asaas.com/i/annual001",
                        "https://sandbox.asaas.com/b/pdf/annual001",
                        "34191.75600 99999.000001 00000.000001 1 00000000089900"
                ));

        mockMvc.perform(post(ENDPOINT)
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(checkoutBody("starter", "boleto", "annual")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentMethod", is("boleto")))
                .andExpect(jsonPath("$.boletoCode").exists());
    }

    @Test
    void returnsUnauthorizedWhenNoToken() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(APPLICATION_JSON)
                        .content(checkoutBody("starter", "boleto", "monthly")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returns503WhenGatewayNotConfigured() throws Exception {
        UUID farmId = insertFarm("Fazenda Sem Config");
        UserAccount user = insertUser(farmId, "5511900000004", "Admin Cfg", UserRole.ADMIN, true);
        String token = issueAccessToken(user, OffsetDateTime.now(ZoneOffset.UTC));

        when(asaasGateway.createOrFindCustomer(anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new BillingGatewayException("API key not configured",
                        BillingGatewayException.Kind.NOT_CONFIGURED));

        mockMvc.perform(post(ENDPOINT)
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(checkoutBody("starter", "boleto", "monthly")))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void returns502WhenGatewayServerError() throws Exception {
        UUID farmId = insertFarm("Fazenda Erro Gateway");
        UserAccount user = insertUser(farmId, "5511900000005", "Admin Err", UserRole.ADMIN, true);
        String token = issueAccessToken(user, OffsetDateTime.now(ZoneOffset.UTC));

        when(asaasGateway.createOrFindCustomer(anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new BillingGatewayException("Gateway error",
                        BillingGatewayException.Kind.SERVER_ERROR));

        mockMvc.perform(post(ENDPOINT)
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(checkoutBody("starter", "boleto", "monthly")))
                .andExpect(status().isBadGateway());
    }

    @Test
    void returns400WhenGatewayClientError() throws Exception {
        UUID farmId = insertFarm("Fazenda Dados Ruins");
        UserAccount user = insertUser(farmId, "5511900000006", "Admin Bad", UserRole.ADMIN, true);
        String token = issueAccessToken(user, OffsetDateTime.now(ZoneOffset.UTC));

        when(asaasGateway.createOrFindCustomer(anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new BillingGatewayException("Invalid data",
                        BillingGatewayException.Kind.CLIENT_ERROR));

        mockMvc.perform(post(ENDPOINT)
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(checkoutBody("starter", "boleto", "monthly")))
                .andExpect(status().isBadRequest());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static String checkoutBody(String plan, String paymentMethod, String billingCycle) {
        return """
                {
                  "plan": "%s",
                  "paymentMethod": "%s",
                  "billingCycle": "%s",
                  "customerType": "company",
                  "name": "Fazenda Teste LTDA",
                  "document": "11222333000181",
                  "email": "financeiro@fazenda.com",
                  "phone": "+5511999990001",
                  "postalCode": "74000000",
                  "address": "Rodovia Rural",
                  "addressNumber": "100",
                  "city": "Goiânia",
                  "state": "GO"
                }
                """.formatted(plan, paymentMethod, billingCycle);
    }

    private static String checkoutBodyWithCard(String plan, String paymentMethod, String billingCycle) {
        return """
                {
                  "plan": "%s",
                  "paymentMethod": "%s",
                  "billingCycle": "%s",
                  "customerType": "company",
                  "name": "Fazenda Teste LTDA",
                  "document": "11222333000181",
                  "email": "financeiro@fazenda.com",
                  "phone": "+5511999990001",
                  "postalCode": "74000000",
                  "address": "Rodovia Rural",
                  "addressNumber": "100",
                  "city": "Goiânia",
                  "state": "GO",
                  "cardHolderName": "Fazenda Teste LTDA",
                  "cardNumber": "4111111111111111",
                  "cardExpiryMonth": "12",
                  "cardExpiryYear": "2030",
                  "cardCcv": "123"
                }
                """.formatted(plan, paymentMethod, billingCycle);
    }
}
