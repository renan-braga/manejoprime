package br.com.curralzap.backend.auth.infrastructure;

import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.auth.application.AuthTokenPair;
import br.com.curralzap.backend.auth.application.AuthTokenType;
import br.com.curralzap.backend.auth.application.ExpiredAuthTokenException;
import br.com.curralzap.backend.auth.application.InvalidAuthTokenException;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtAuthTokenPairIssuerTest {

    private static final String SECRET = "test-secret-for-auth-jwt-issuer-123456789";
    private static final Duration ACCESS_TOKEN_TTL = Duration.ofHours(8);

    @Test
    void issuesAccessAndRefreshTokensWithExpectedClaims() {
        JwtAuthTokenPairIssuer issuer = new JwtAuthTokenPairIssuer(new ObjectMapper(), SECRET, ACCESS_TOKEN_TTL);
        UserAccount userAccount = userAccount();
        UUID sessionId = UUID.randomUUID();
        UUID refreshTokenId = UUID.randomUUID();
        OffsetDateTime issuedAt = OffsetDateTime.of(2026, 3, 24, 12, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime refreshExpiresAt = issuedAt.plusDays(30);

        AuthTokenPair tokenPair = issuer.issueFor(userAccount, sessionId, refreshTokenId, issuedAt, refreshExpiresAt);

        AuthTokenClaims accessClaims = issuer.verify(tokenPair.accessToken(), AuthTokenType.ACCESS, issuedAt.plusMinutes(1));
        AuthTokenClaims refreshClaims = issuer.verify(tokenPair.refreshToken(), AuthTokenType.REFRESH, issuedAt.plusMinutes(1));

        assertThat(accessClaims.userId()).isEqualTo(userAccount.id());
        assertThat(accessClaims.farmId()).isEqualTo(userAccount.farmId());
        assertThat(accessClaims.role()).isEqualTo(userAccount.role());
        assertThat(accessClaims.sessionId()).isEqualTo(sessionId);
        assertThat(accessClaims.type()).isEqualTo(AuthTokenType.ACCESS);
        assertThat(accessClaims.expiresAt()).isEqualTo(issuedAt.plusHours(8));
        assertThat(refreshClaims.userId()).isEqualTo(userAccount.id());
        assertThat(refreshClaims.farmId()).isEqualTo(userAccount.farmId());
        assertThat(refreshClaims.role()).isEqualTo(userAccount.role());
        assertThat(refreshClaims.sessionId()).isEqualTo(sessionId);
        assertThat(refreshClaims.tokenId()).isEqualTo(refreshTokenId);
        assertThat(refreshClaims.type()).isEqualTo(AuthTokenType.REFRESH);
    }

    @Test
    void rejectsTamperedTokenSignature() {
        JwtAuthTokenPairIssuer issuer = new JwtAuthTokenPairIssuer(new ObjectMapper(), SECRET, ACCESS_TOKEN_TTL);
        AuthTokenPair tokenPair = issuer.issueFor(
                userAccount(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                OffsetDateTime.of(2026, 3, 24, 12, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 4, 23, 12, 0, 0, 0, ZoneOffset.UTC)
        );

        String tamperedToken = tokenPair.refreshToken().substring(0, tokenPair.refreshToken().length() - 1) + "x";

        assertThatThrownBy(() -> issuer.verify(
                tamperedToken,
                AuthTokenType.REFRESH,
                OffsetDateTime.of(2026, 3, 24, 12, 1, 0, 0, ZoneOffset.UTC)
        )).isInstanceOf(InvalidAuthTokenException.class);
    }

    @Test
    void rejectsTokenWhenTypeDoesNotMatchExpectedType() {
        JwtAuthTokenPairIssuer issuer = new JwtAuthTokenPairIssuer(new ObjectMapper(), SECRET, ACCESS_TOKEN_TTL);
        AuthTokenPair tokenPair = issuer.issueFor(
                userAccount(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                OffsetDateTime.of(2026, 3, 24, 12, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 4, 23, 12, 0, 0, 0, ZoneOffset.UTC)
        );

        assertThatThrownBy(() -> issuer.verify(
                tokenPair.refreshToken(),
                AuthTokenType.ACCESS,
                OffsetDateTime.of(2026, 3, 24, 12, 1, 0, 0, ZoneOffset.UTC)
        )).isInstanceOf(InvalidAuthTokenException.class);
    }

    @Test
    void rejectsExpiredToken() {
        JwtAuthTokenPairIssuer issuer = new JwtAuthTokenPairIssuer(new ObjectMapper(), SECRET, ACCESS_TOKEN_TTL);
        OffsetDateTime issuedAt = OffsetDateTime.of(2026, 3, 24, 10, 0, 0, 0, ZoneOffset.UTC);
        AuthTokenPair tokenPair = issuer.issueFor(
                userAccount(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                issuedAt,
                issuedAt.plusDays(30)
        );

        assertThatThrownBy(() -> issuer.verify(
                tokenPair.accessToken(),
                AuthTokenType.ACCESS,
                issuedAt.plusHours(8).plusSeconds(1)
        )).isInstanceOf(ExpiredAuthTokenException.class);
    }

    private static UserAccount userAccount() {
        return new UserAccount(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "5511999990001",
                "admin@test.local",
                "Admin User",
                UserRole.ADMIN,
                true,
                OffsetDateTime.now(ZoneOffset.UTC),
                OffsetDateTime.now(ZoneOffset.UTC)
        );
    }
}
