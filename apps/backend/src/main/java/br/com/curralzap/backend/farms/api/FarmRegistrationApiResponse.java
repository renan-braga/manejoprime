package br.com.curralzap.backend.farms.api;

import br.com.curralzap.backend.auth.application.AuthTokenPair;

import java.time.OffsetDateTime;

record FarmRegistrationApiResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        OffsetDateTime accessTokenExpiresAt,
        OffsetDateTime refreshTokenExpiresAt
) {

    static FarmRegistrationApiResponse from(AuthTokenPair tokenPair) {
        return new FarmRegistrationApiResponse(
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                tokenPair.tokenType(),
                tokenPair.accessTokenExpiresAt(),
                tokenPair.refreshTokenExpiresAt()
        );
    }
}
