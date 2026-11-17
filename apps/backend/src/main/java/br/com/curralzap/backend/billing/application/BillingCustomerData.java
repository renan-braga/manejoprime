package br.com.curralzap.backend.billing.application;

import java.util.UUID;

public record BillingCustomerData(
        UUID farmId,
        String customerType,
        String name,
        String document,
        String email,
        String phone,
        String postalCode,
        String address,
        String addressNumber,
        String city,
        String state
) {}
