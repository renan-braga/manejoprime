package br.com.curralzap.backend.billing.infrastructure;

import br.com.curralzap.backend.billing.application.AsaasGateway;
import br.com.curralzap.backend.billing.application.BillingGatewayException;
import br.com.curralzap.backend.billing.application.BillingGatewayException.Kind;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
class AsaasApiClient implements AsaasGateway {

    private static final Logger log = LoggerFactory.getLogger(AsaasApiClient.class);

    private final RestClient restClient;
    private final boolean apiKeyConfigured;

    AsaasApiClient(
            RestClient.Builder restClientBuilder,
            @Value("${curralzap.asaas.api-key:}") String apiKey,
            @Value("${curralzap.asaas.base-url:https://www.asaas.com/api/v3}") String baseUrl
    ) {
        String key = apiKey == null ? "" : apiKey;
        this.apiKeyConfigured = !key.isBlank();
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("access_token", key)
                .build();
    }

    private void requireApiKey() {
        if (!apiKeyConfigured) {
            throw new BillingGatewayException("Asaas API key is not configured.", Kind.NOT_CONFIGURED);
        }
    }

    private BillingGatewayException wrapAsaasError(String context, RestClientException e) {
        if (e instanceof RestClientResponseException re) {
            int status = re.getStatusCode().value();
            log.warn("Asaas: {} — HTTP {} body={}", context, status, re.getResponseBodyAsString());
            Kind kind = status >= 500 ? Kind.SERVER_ERROR : Kind.CLIENT_ERROR;
            return new BillingGatewayException(context + " — HTTP " + status, kind, e);
        }
        log.warn("Asaas: {} — {}", context, e.getMessage());
        return new BillingGatewayException(context, Kind.SERVER_ERROR, e);
    }

    // ── Customer ──────────────────────────────────────────────────────────────

    @Override
    public String createOrFindCustomer(
            String name, String cpfCnpj, String email, String phone,
            String postalCode, String address, String addressNumber,
            String city, String state, String personType
    ) {
        requireApiKey();
        String cleanDoc = cpfCnpj == null ? "" : cpfCnpj.replaceAll("[^\\d]", "");
        String cleanPhone = phone == null ? "" : phone.replaceAll("[^\\d]", "");
        if (cleanPhone.length() == 13 && cleanPhone.startsWith("55")) cleanPhone = cleanPhone.substring(2);
        phone = cleanPhone;
        String customerId = null;

        if (!cleanDoc.isBlank()) {
            try {
                CustomerListResponse existing = restClient.get()
                        .uri("/customers?cpfCnpj={cpfCnpj}&limit=1", cleanDoc)
                        .retrieve()
                        .body(CustomerListResponse.class);
                if (existing != null && existing.data() != null && !existing.data().isEmpty()) {
                    customerId = existing.data().get(0).id();
                    log.info("Asaas: found existing customer id={} cpfCnpj={}", customerId, cleanDoc);
                }
            } catch (RestClientException e) {
                log.warn("Asaas: customer lookup failed, proceeding with create — {}", e.getMessage());
            }
        }

        if (customerId == null) {
            var createBody = new CustomerRequest(name, cleanDoc, email, phone,
                    postalCode, address, addressNumber, city, state, personType);
            CustomerResponse response;
            try {
                response = restClient.post()
                        .uri("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(createBody)
                        .retrieve()
                        .body(CustomerResponse.class);
            } catch (RestClientException e) {
                throw wrapAsaasError("customer creation failed", e);
            }
            if (response == null || response.id() == null) {
                throw new BillingGatewayException("Asaas customer creation returned no id");
            }
            customerId = response.id();
            log.info("Asaas: created customer id={}", customerId);
        }

        // Always PUT to keep customer data in sync
        var updateBody = new CustomerRequest(name, cleanDoc, email, phone,
                postalCode, address, addressNumber, city, state, personType);
        try {
            restClient.put()
                    .uri("/customers/{id}", customerId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(updateBody)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Asaas: updated customer id={}", customerId);
        } catch (RestClientResponseException e) {
            log.warn("Asaas: customer update failed id={} — HTTP {} body={}",
                    customerId, e.getStatusCode().value(), e.getResponseBodyAsString());
        } catch (RestClientException e) {
            log.warn("Asaas: customer update failed id={} — {}", customerId, e.getMessage());
        }

        return customerId;
    }

    // ── Subscription ──────────────────────────────────────────────────────────

    @Override
    public SubscriptionResult createSubscription(
            String customerId, String billingType, BigDecimal value,
            LocalDate nextDueDate, String description, String cycle,
            CreditCardData creditCard, CreditCardHolderInfo holderInfo
    ) {
        boolean directCardCharge = "CREDIT_CARD".equals(billingType) && creditCard != null;

        CreditCardRequest cardReq = creditCard != null
                ? new CreditCardRequest(creditCard.holderName(), creditCard.number(),
                        creditCard.expiryMonth(), creditCard.expiryYear(), creditCard.ccv())
                : null;

        CreditCardHolderInfoRequest holderReq = holderInfo != null
                ? new CreditCardHolderInfoRequest(holderInfo.name(), holderInfo.email(),
                        holderInfo.cpfCnpj(), holderInfo.postalCode(),
                        holderInfo.addressNumber(), holderInfo.phone())
                : null;

        String remoteIp = holderInfo != null ? holderInfo.remoteIp() : null;
        var body = new SubscriptionRequest(customerId, billingType, value,
                nextDueDate.toString(), cycle, description, cardReq, holderReq, remoteIp);

        SubscriptionResponse response;
        try {
            response = restClient.post()
                    .uri("/subscriptions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(SubscriptionResponse.class);
        } catch (RestClientResponseException e) {
            log.warn("Asaas: subscription creation failed — HTTP {} body={}", e.getStatusCode().value(), e.getResponseBodyAsString());
            // For direct card charge, do not try recovery — the error is about the card itself
            if (!directCardCharge && e.getStatusCode().value() == 400) {
                SubscriptionResult recovered = recoverExistingSubscription(customerId, billingType);
                if (recovered != null) {
                    log.info("Asaas: recovered existing subscription for customer={}", customerId);
                    return recovered;
                }
            }
            throw wrapAsaasError("subscription creation failed", e);
        } catch (RestClientException e) {
            throw wrapAsaasError("subscription creation failed", e);
        }
        if (response == null || response.id() == null) {
            throw new BillingGatewayException("Asaas subscription creation returned no id");
        }

        if (directCardCharge) {
            log.info("Asaas: created CREDIT_CARD subscription id={} (direct charge)", response.id());
            return SubscriptionResult.cardCharged(response.id());
        }

        PaymentResponse payment = fetchFirstPayment(response.id());
        if (payment == null || payment.invoiceUrl() == null) {
            throw new BillingGatewayException("Asaas subscription has no payment URL", Kind.SERVER_ERROR);
        }

        log.info("Asaas: created subscription id={} billingType={} paymentId={}", response.id(), billingType, payment.id());
        return buildSubscriptionResult(response.id(), billingType, payment);
    }

    private SubscriptionResult buildSubscriptionResult(String subscriptionId, String billingType, PaymentResponse payment) {
        return switch (billingType) {
            case "BOLETO" -> {
                String boletoCode = fetchBoletoIdentificationField(payment.id());
                yield SubscriptionResult.boleto(subscriptionId, payment.invoiceUrl(), payment.bankSlipUrl(), boletoCode);
            }
            case "PIX" -> {
                PixQrCodeResponse pix = fetchPixQrCode(payment.id());
                String qrImage = pix != null ? "data:image/png;base64," + pix.encodedImage() : null;
                String copyPaste = pix != null ? pix.payload() : null;
                yield SubscriptionResult.pix(subscriptionId, payment.invoiceUrl(), qrImage, copyPaste);
            }
            default -> SubscriptionResult.card(subscriptionId, payment.invoiceUrl());
        };
    }

    private SubscriptionResult recoverExistingSubscription(String customerId, String billingType) {
        try {
            SubscriptionListResponse resp = restClient.get()
                    .uri("/subscriptions?customer={id}&billingType={bt}&status=ACTIVE&limit=1", customerId, billingType)
                    .retrieve()
                    .body(SubscriptionListResponse.class);
            if (resp != null && resp.data() != null && !resp.data().isEmpty()) {
                String subId = resp.data().get(0).id();
                PaymentResponse payment = fetchFirstPayment(subId);
                if (payment != null) {
                    return buildSubscriptionResult(subId, billingType, payment);
                }
            }
        } catch (RestClientException e) {
            log.warn("Asaas: recoverExistingSubscription failed — {}", e.getMessage());
        }
        return null;
    }

    // ── Payment detail fetchers ───────────────────────────────────────────────

    private PaymentResponse fetchFirstPayment(String subscriptionId) {
        try {
            PaymentListResponse resp = restClient.get()
                    .uri("/payments?subscription={id}&limit=1", subscriptionId)
                    .retrieve()
                    .body(PaymentListResponse.class);
            if (resp != null && resp.data() != null && !resp.data().isEmpty()) {
                return resp.data().get(0);
            }
        } catch (RestClientException e) {
            log.warn("Asaas: fetchFirstPayment failed for sub={} — {}", subscriptionId, e.getMessage());
        }
        return null;
    }

    private String fetchBoletoIdentificationField(String paymentId) {
        try {
            BoletoFieldResponse resp = restClient.get()
                    .uri("/payments/{id}/identificationField", paymentId)
                    .retrieve()
                    .body(BoletoFieldResponse.class);
            return resp != null ? resp.identificationField() : null;
        } catch (RestClientException e) {
            log.warn("Asaas: fetchBoletoIdentificationField failed for pay={} — {}", paymentId, e.getMessage());
            return null;
        }
    }

    private PixQrCodeResponse fetchPixQrCode(String paymentId) {
        try {
            return restClient.get()
                    .uri("/payments/{id}/pixQrCode", paymentId)
                    .retrieve()
                    .body(PixQrCodeResponse.class);
        } catch (RestClientException e) {
            log.warn("Asaas: fetchPixQrCode failed for pay={} — {}", paymentId, e.getMessage());
            return null;
        }
    }

    // ── DTOs ──────────────────────────────────────────────────────────────────

    private record CustomerRequest(
            String name,
            @JsonProperty("cpfCnpj") String cpfCnpj,
            String email,
            @JsonProperty("mobilePhone") String mobilePhone,
            @JsonProperty("postalCode") String postalCode,
            String address,
            @JsonProperty("addressNumber") String addressNumber,
            String city,
            String state,
            @JsonProperty("personType") String personType
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CustomerResponse(String id) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CustomerListResponse(List<CustomerResponse> data) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record SubscriptionRequest(
            String customer,
            @JsonProperty("billingType") String billingType,
            BigDecimal value,
            @JsonProperty("nextDueDate") String nextDueDate,
            String cycle,
            String description,
            @JsonProperty("creditCard") CreditCardRequest creditCard,
            @JsonProperty("creditCardHolderInfo") CreditCardHolderInfoRequest creditCardHolderInfo,
            @JsonProperty("remoteIp") String remoteIp
    ) {}

    private record CreditCardRequest(
            @JsonProperty("holderName") String holderName,
            String number,
            @JsonProperty("expiryMonth") String expiryMonth,
            @JsonProperty("expiryYear") String expiryYear,
            String ccv
    ) {}

    private record CreditCardHolderInfoRequest(
            String name,
            String email,
            @JsonProperty("cpfCnpj") String cpfCnpj,
            @JsonProperty("postalCode") String postalCode,
            @JsonProperty("addressNumber") String addressNumber,
            String phone
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SubscriptionResponse(String id) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SubscriptionListResponse(List<SubscriptionResponse> data) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PaymentResponse(
            String id,
            @JsonProperty("invoiceUrl") String invoiceUrl,
            @JsonProperty("bankSlipUrl") String bankSlipUrl
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PaymentListResponse(List<PaymentResponse> data) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record BoletoFieldResponse(
            @JsonProperty("identificationField") String identificationField
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PixQrCodeResponse(
            @JsonProperty("encodedImage") String encodedImage,
            @JsonProperty("payload") String payload
    ) {}
}
