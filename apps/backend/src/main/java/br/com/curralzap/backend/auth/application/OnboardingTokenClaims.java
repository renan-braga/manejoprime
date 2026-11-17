package br.com.curralzap.backend.auth.application;

import java.time.OffsetDateTime;

public record OnboardingTokenClaims(String email, String name, OffsetDateTime expiresAt) {
}
