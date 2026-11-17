package br.com.curralzap.backend.auth.application;

import java.time.OffsetDateTime;

public interface AuthTokenParser {

    AuthTokenClaims verify(String token, AuthTokenType expectedType, OffsetDateTime now);
}
