package br.com.curralzap.backend.billing.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

@Service
public class BillingCheckoutService {

    private static final Map<String, BigDecimal> PLAN_PRICES = Map.of(
            "starter",  new BigDecimal("89.90"),
            "pro",      new BigDecimal("169.90"),
            "business", new BigDecimal("319.90")
    );

    private static final Map<String, BigDecimal> ANNUAL_PLAN_PRICES = Map.of(
            "starter",  new BigDecimal("899.00"),
            "pro",      new BigDecimal("1699.00"),
            "business", new BigDecimal("3199.00")
    );

    private static final Map<String, String> BILLING_TYPE_MAP = Map.of(
            "pix",    "PIX",
            "boleto", "BOLETO",
            "card",   "CREDIT_CARD",
            "undefined", "UNDEFINED"
    );

    private final AsaasGateway asaasGateway;
    private final BillingSubscriptionRepository subscriptionRepository;

    public BillingCheckoutService(
            AsaasGateway asaasGateway,
            BillingSubscriptionRepository subscriptionRepository
    ) {
        this.asaasGateway = asaasGateway;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Transactional
    public CheckoutResult checkout(UUID farmId, CheckoutRequest request) {
        boolean annual = "annual".equalsIgnoreCase(request.billingCycle());
        Map<String, BigDecimal> prices = annual ? ANNUAL_PLAN_PRICES : PLAN_PRICES;
        BigDecimal price = prices.get(request.plan());
        if (price == null) {
            throw new IllegalArgumentException("Unknown plan: " + request.plan());
        }

        String asaasBillingType = BILLING_TYPE_MAP.getOrDefault(request.billingType(), "PIX");
        String asaasCycle = annual ? "YEARLY" : "MONTHLY";
        String personType = "company".equalsIgnoreCase(request.customerType()) ? "JURIDICA" : "FISICA";

        String customerId = asaasGateway.createOrFindCustomer(
                request.name(), request.document(), request.email(), request.phone(),
                request.postalCode(), request.address(), request.addressNumber(),
                request.city(), request.state(), personType
        );

        AsaasGateway.CreditCardData creditCard = null;
        AsaasGateway.CreditCardHolderInfo holderInfo = null;
        if ("CREDIT_CARD".equals(asaasBillingType) && request.cardHolderName() != null) {
            creditCard = new AsaasGateway.CreditCardData(
                    request.cardHolderName(),
                    request.cardNumber() != null ? request.cardNumber().replaceAll("\\D", "") : "",
                    request.cardExpiryMonth(),
                    request.cardExpiryYear(),
                    request.cardCcv()
            );
            holderInfo = new AsaasGateway.CreditCardHolderInfo(
                    request.name(),
                    request.email(),
                    request.document() != null ? request.document().replaceAll("\\D", "") : "",
                    request.postalCode() != null ? request.postalCode().replaceAll("\\D", "") : "",
                    request.addressNumber(),
                    request.phone(),
                    request.clientIp()
            );
        }

        String planLabel = Character.toUpperCase(request.plan().charAt(0)) + request.plan().substring(1);
        String description = "ManejoPrime — Plano " + planLabel + (annual ? " Anual" : "");
        AsaasGateway.SubscriptionResult sub = asaasGateway.createSubscription(
                customerId, asaasBillingType, price, LocalDate.now(ZoneOffset.UTC),
                description, asaasCycle, creditCard, holderInfo
        );

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        subscriptionRepository.upsertForCheckout(
                farmId, request.plan(), "asaas", customerId, sub.subscriptionId(), now
        );

        boolean directlyCharged = "CREDIT_CARD".equals(asaasBillingType) && sub.invoiceUrl() == null;
        if (directlyCharged) {
            OffsetDateTime periodEnd = annual ? now.plusYears(1) : now.plusMonths(1);
            subscriptionRepository.activateDirectCharge(farmId, periodEnd, now);
        }

        String paymentMethod = switch (asaasBillingType) {
            case "PIX" -> "pix";
            case "BOLETO" -> "boleto";
            default -> "card";
        };

        return new CheckoutResult(
                paymentMethod,
                sub.invoiceUrl(),
                sub.bankSlipUrl(),
                sub.boletoCode(),
                sub.pixQrCodeImage(),
                sub.pixCopyPaste()
        );
    }
}
