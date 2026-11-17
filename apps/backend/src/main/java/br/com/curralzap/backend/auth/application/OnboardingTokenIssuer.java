package br.com.curralzap.backend.auth.application;

import java.time.OffsetDateTime;

public interface OnboardingTokenIssuer {
    String issue(OAuthIdentity identity, OffsetDateTime issuedAt, OffsetDateTime expiresAt);
}
