package br.com.curralzap.backend.farms.api;

import br.com.curralzap.backend.auth.application.AuthTokenPair;

import java.time.OffsetDateTime;

record FarmSwitchApiResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        OffsetDateTime accessTokenExpiresAt,
        OffsetDateTime refreshTokenExpiresAt
) {

    static FarmSwitchApiResponse from(AuthTokenPair tokenPair) {
        return new FarmSwitchApiResponse(
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                tokenPair.tokenType(),
                tokenPair.accessTokenExpiresAt(),
                tokenPair.refreshTokenExpiresAt()
        );
    }
}
