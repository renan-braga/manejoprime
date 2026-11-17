package br.com.curralzap.backend.billing.application;

public record CheckoutRequest(
        String plan,
        String billingType,
        String billingCycle,
        String customerType,
        String name,
        String document,
        String email,
        String phone,
        String postalCode,
        String address,
        String addressNumber,
        String city,
        String state,
        // Credit card — null for non-card billing types
        String cardHolderName,
        String cardNumber,
        String cardExpiryMonth,
        String cardExpiryYear,
        String cardCcv,
        String clientIp
) {}
