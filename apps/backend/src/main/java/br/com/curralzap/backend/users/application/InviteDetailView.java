package br.com.curralzap.backend.users.application;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InviteDetailView(
        UUID id,
        UUID farmId,
        String email,
        String phone,
        String role,
        String status,
        UUID invitedBy,
        OffsetDateTime createdAt
) {
}
