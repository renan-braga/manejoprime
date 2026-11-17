package br.com.curralzap.backend.auth.api;

import br.com.curralzap.backend.auth.application.AuthTokenPair;

import java.time.OffsetDateTime;

record AuthTokenApiResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        OffsetDateTime accessTokenExpiresAt,
        OffsetDateTime refreshTokenExpiresAt
) {

    static AuthTokenApiResponse from(AuthTokenPair result) {
        return new AuthTokenApiResponse(
                result.accessToken(),
                result.refreshToken(),
                result.tokenType(),
                result.accessTokenExpiresAt(),
                result.refreshTokenExpiresAt()
        );
    }
}
