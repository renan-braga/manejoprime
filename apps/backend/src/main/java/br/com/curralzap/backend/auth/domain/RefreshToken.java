package br.com.curralzap.backend.auth.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RefreshToken(
        UUID id,
        UUID sessionId,
        UUID userId,
        UUID farmId,
        OffsetDateTime expiresAt,
        OffsetDateTime consumedAt,
        OffsetDateTime revokedAt,
        SessionRevocationReason revocationReason,
        UUID replacedByTokenId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static RefreshToken issue(
            UUID tokenId,
            UUID sessionId,
            UUID userId,
            UUID farmId,
            OffsetDateTime issuedAt,
            OffsetDateTime expiresAt
    ) {
        return new RefreshToken(tokenId, sessionId, userId, farmId, expiresAt, null, null, null, null, issuedAt, issuedAt);
    }

    public boolean isExpiredAt(OffsetDateTime now) {
        return expiresAt.isBefore(now);
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isReplacedByNewLogin() {
        return revocationReason == SessionRevocationReason.REPLACED_BY_NEW_LOGIN;
    }

    public RefreshToken markConsumed(OffsetDateTime consumedAt, UUID replacedByTokenId) {
        return new RefreshToken(
                id,
                sessionId,
                userId,
                farmId,
                expiresAt,
                consumedAt,
                revokedAt,
                revocationReason,
                replacedByTokenId,
                createdAt,
                consumedAt
        );
    }

    public RefreshToken revoke(SessionRevocationReason reason, OffsetDateTime revokedAt) {
        return new RefreshToken(
                id,
                sessionId,
                userId,
                farmId,
                expiresAt,
                consumedAt,
                revokedAt,
                reason,
                replacedByTokenId,
                createdAt,
                revokedAt
        );
    }
}
