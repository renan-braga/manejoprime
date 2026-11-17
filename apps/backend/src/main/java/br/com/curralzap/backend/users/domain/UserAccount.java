package br.com.curralzap.backend.users.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserAccount(
        UUID id,
        UUID farmId,
        String phone,
        String email,
        String name,
        UserRole role,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static UserAccount create(UUID farmId, String phone, String email, String name, UserRole role) {
        return new UserAccount(null, farmId, phone, email, name, role, true, null, null);
    }

    public UserAccount update(String phone, String name, UserRole role) {
        return new UserAccount(id, farmId, phone, email, name, role, active, createdAt, updatedAt);
    }

    public UserAccount withPhone(String phone) {
        return new UserAccount(id, farmId, phone, email, name, role, active, createdAt, updatedAt);
    }

    public UserAccount withFarmId(UUID newFarmId) {
        return new UserAccount(id, newFarmId, phone, email, name, role, active, createdAt, updatedAt);
    }

    public UserAccount activate() {
        return withActive(true);
    }

    public UserAccount deactivate() {
        return withActive(false);
    }

    private UserAccount withActive(boolean active) {
        return new UserAccount(id, farmId, phone, email, name, role, active, createdAt, updatedAt);
    }
}
