package br.com.curralzap.backend.auth.application;

import br.com.curralzap.backend.users.domain.UserAccount;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface AuthTokenPairIssuer {

    AuthTokenPair issueFor(
            UserAccount userAccount,
            UUID sessionId,
            UUID refreshTokenId,
            OffsetDateTime issuedAt,
            OffsetDateTime refreshTokenExpiresAt
    );
}
