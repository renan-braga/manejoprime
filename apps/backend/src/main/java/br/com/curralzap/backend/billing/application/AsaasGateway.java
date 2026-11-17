package br.com.curralzap.backend.billing.application;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface AsaasGateway {

    String createOrFindCustomer(
            String name,
            String cpfCnpj,
            String email,
            String phone,
            String postalCode,
            String address,
            String addressNumber,
            String city,
            String state,
            String personType
    );

    record CreditCardData(
            String holderName,
            String number,
            String expiryMonth,
            String expiryYear,
            String ccv
    ) {}

    record CreditCardHolderInfo(
            String name,
            String email,
            String cpfCnpj,
            String postalCode,
            String addressNumber,
            String phone,
            String remoteIp
    ) {}

    record SubscriptionResult(
            String subscriptionId,
            String billingType,
            String invoiceUrl,
            String bankSlipUrl,
            String boletoCode,
            String pixQrCodeImage,
            String pixCopyPaste
    ) {
        public static SubscriptionResult boleto(String subscriptionId, String invoiceUrl, String bankSlipUrl, String boletoCode) {
            return new SubscriptionResult(subscriptionId, "BOLETO", invoiceUrl, bankSlipUrl, boletoCode, null, null);
        }

        public static SubscriptionResult pix(String subscriptionId, String invoiceUrl, String pixQrCodeImage, String pixCopyPaste) {
            return new SubscriptionResult(subscriptionId, "PIX", invoiceUrl, null, null, pixQrCodeImage, pixCopyPaste);
        }

        public static SubscriptionResult card(String subscriptionId, String invoiceUrl) {
            return new SubscriptionResult(subscriptionId, "CREDIT_CARD", invoiceUrl, null, null, null, null);
        }

        /** Card was charged directly — no redirect URL needed. */
        public static SubscriptionResult cardCharged(String subscriptionId) {
            return new SubscriptionResult(subscriptionId, "CREDIT_CARD", null, null, null, null, null);
        }
    }

    SubscriptionResult createSubscription(
            String customerId,
            String billingType,
            BigDecimal value,
            LocalDate nextDueDate,
            String description,
            String cycle,
            CreditCardData creditCard,
            CreditCardHolderInfo holderInfo
    );
}
