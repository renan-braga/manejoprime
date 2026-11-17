package br.com.curralzap.backend.auth.application;

import java.time.OffsetDateTime;

public record AuthTokenPair(
        String accessToken,
        String refreshToken,
        String tokenType,
        OffsetDateTime accessTokenExpiresAt,
        OffsetDateTime refreshTokenExpiresAt
) {
}
