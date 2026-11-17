package br.com.curralzap.backend.auth.application;

import java.time.OffsetDateTime;

public interface OnboardingTokenParser {
    OnboardingTokenClaims verify(String token, OffsetDateTime now);
}
