package br.com.curralzap.backend.auth.application;

import br.com.curralzap.backend.billing.application.BillingSnapshot;

import java.util.List;
import java.util.UUID;

public record SessionMeView(
        UUID userId,
        String name,
        String phone,
        String email,
        String role,
        UUID farmId,
        String farmName,
        List<String> permissions,
        BillingSnapshot billing,
        int farmCount,
        long animalCount,
        int userCount
) {

    public SessionMeView withBilling(BillingSnapshot billing) {
        return new SessionMeView(userId, name, phone, email, role, farmId, farmName, permissions, billing, farmCount, animalCount, userCount);
    }

    public SessionMeView withFarmCount(int count) {
        return new SessionMeView(userId, name, phone, email, role, farmId, farmName, permissions, billing, count, animalCount, userCount);
    }
}
