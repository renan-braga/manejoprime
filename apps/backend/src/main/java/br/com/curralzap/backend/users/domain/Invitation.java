package br.com.curralzap.backend.users.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Invitation(
        UUID id,
        UUID farmId,
        String email,
        String phone,
        UserRole role,
        InvitationStatus status,
        UUID invitedBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static Invitation create(UUID farmId, String email, String phone, UserRole role, UUID invitedBy) {
        return new Invitation(null, farmId, email, phone, role, InvitationStatus.PENDING, invitedBy, null, null);
    }

    public Invitation accept() {
        return new Invitation(id, farmId, email, phone, role, InvitationStatus.ACCEPTED, invitedBy, createdAt, updatedAt);
    }

    public Invitation revoke() {
        return new Invitation(id, farmId, email, phone, role, InvitationStatus.REVOKED, invitedBy, createdAt, updatedAt);
    }
}
