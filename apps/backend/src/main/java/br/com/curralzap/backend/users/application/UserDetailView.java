package br.com.curralzap.backend.users.application;

import java.util.UUID;

public record UserDetailView(
        UUID id,
        UUID farmId,
        String name,
        String phone,
        String role,
        boolean active
) {
}
