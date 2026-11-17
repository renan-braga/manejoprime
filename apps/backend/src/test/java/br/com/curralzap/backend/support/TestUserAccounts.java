package br.com.curralzap.backend.support;

import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserRole;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class TestUserAccounts {

    private TestUserAccounts() {
    }

    public static UserAccount userAccount(
            UUID farmId,
            String phone,
            String name,
            UserRole role,
            boolean active,
            OffsetDateTime timestamp
    ) {
        return new UserAccount(
                UUID.randomUUID(),
                farmId,
                phone.replaceFirst("^\\+", ""),
                null,
                name,
                role,
                active,
                timestamp,
                timestamp
        );
    }

    public static UserAccount userAccount(
            String phone,
            String name,
            UserRole role,
            boolean active,
            OffsetDateTime timestamp
    ) {
        return userAccount(UUID.randomUUID(), phone, name, role, active, timestamp);
    }
}
