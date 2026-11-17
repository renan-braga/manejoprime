package br.com.curralzap.backend.auth.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuthSession(
        UUID id,
        UUID userId,
        UUID farmId,
        OffsetDateTime revokedAt,
        SessionRevocationReason revocationReason,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static AuthSession start(UUID userId, UUID farmId, OffsetDateTime startedAt) {
        return new AuthSession(null, userId, farmId, null, null, startedAt, startedAt);
    }

    public boolean isActive() {
        return revokedAt == null;
    }

    public boolean isReplacedByNewLogin() {
        return revocationReason == SessionRevocationReason.REPLACED_BY_NEW_LOGIN;
    }

    public AuthSession revoke(SessionRevocationReason reason, OffsetDateTime revokedAt) {
        return new AuthSession(id, userId, farmId, revokedAt, reason, createdAt, revokedAt);
    }
}
