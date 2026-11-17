package br.com.curralzap.backend.auth.application;

import br.com.curralzap.backend.users.domain.UserRole;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuthTokenClaims(
        UUID userId,
        UUID farmId,
        UserRole role,
        UUID sessionId,
        UUID tokenId,
        AuthTokenType type,
        OffsetDateTime issuedAt,
        OffsetDateTime expiresAt
) {
}
