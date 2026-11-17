package br.com.curralzap.backend.auth.api;

import br.com.curralzap.backend.auth.application.AuthTokenPair;
import br.com.curralzap.backend.auth.application.OAuthCallbackResult;

import java.time.OffsetDateTime;

record OAuthCallbackApiResponse(
        String status,
        String accessToken,
        String refreshToken,
        String tokenType,
        OffsetDateTime accessTokenExpiresAt,
        OffsetDateTime refreshTokenExpiresAt,
        String onboardingToken
) {

    static OAuthCallbackApiResponse fromAuthenticated(AuthTokenPair tokenPair) {
        return new OAuthCallbackApiResponse(
                "authenticated",
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                tokenPair.tokenType(),
                tokenPair.accessTokenExpiresAt(),
                tokenPair.refreshTokenExpiresAt(),
                null
        );
    }

    static OAuthCallbackApiResponse fromOnboardingRequired(String onboardingToken) {
        return new OAuthCallbackApiResponse(
                "onboarding_required",
                null, null, null, null, null,
                onboardingToken
        );
    }

    static OAuthCallbackApiResponse from(OAuthCallbackResult result) {
        return switch (result) {
            case OAuthCallbackResult.Authenticated a -> fromAuthenticated(a.tokenPair());
            case OAuthCallbackResult.OnboardingRequired o -> fromOnboardingRequired(o.onboardingToken());
        };
    }
}
