package br.com.curralzap.backend.users.application;

import java.util.UUID;

public record UserListItemView(
        UUID id,
        String name,
        String phone,
        String role,
        boolean active
) {
}
