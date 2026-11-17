package br.com.curralzap.backend.auth.application;

import br.com.curralzap.backend.auth.domain.AuthSession;
import br.com.curralzap.backend.auth.domain.RefreshToken;
import br.com.curralzap.backend.auth.domain.SessionRevocationReason;
import br.com.curralzap.backend.support.InMemoryUserAccountRepository;
import br.com.curralzap.backend.support.TestUserAccounts;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserRole;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthSessionServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-03-24T15:00:00Z"), ZoneOffset.UTC);

    @Test
    void startSessionRevokesPreviousActiveSessionAndRefreshTokens() {
        InMemoryAuthSessionRepository authSessionRepository = new InMemoryAuthSessionRepository();
        InMemoryRefreshTokenRepository refreshTokenRepository = new InMemoryRefreshTokenRepository();
        InMemoryUserAccountRepository userAccountRepository = new InMemoryUserAccountRepository();
        InMemoryAuthTokenCodec authTokenCodec = new InMemoryAuthTokenCodec();
        AuthSessionService service = new AuthSessionService(
                authSessionRepository,
                refreshTokenRepository,
                userAccountRepository,
                new LoginUserPolicy(),
                authTokenCodec,
                authTokenCodec,
                FIXED_CLOCK
        );
        UserAccount userAccount = activeUser();
        userAccountRepository.save(userAccount);

        AuthTokenPair firstSession = service.startSession(userAccount);
        AuthTokenClaims firstClaims = authTokenCodec.verify(
                firstSession.refreshToken(),
                AuthTokenType.REFRESH,
                OffsetDateTime.now(FIXED_CLOCK)
        );

        AuthTokenPair secondSession = service.startSession(userAccount);
        AuthTokenClaims secondClaims = authTokenCodec.verify(
                secondSession.refreshToken(),
                AuthTokenType.REFRESH,
                OffsetDateTime.now(FIXED_CLOCK)
        );

        assertThat(secondClaims.sessionId()).isNotEqualTo(firstClaims.sessionId());
        assertThat(authSessionRepository.findById(firstClaims.sessionId()))
                .isPresent()
                .get()
                .extracting(AuthSession::revocationReason)
                .isEqualTo(SessionRevocationReason.REPLACED_BY_NEW_LOGIN);
        assertThat(refreshTokenRepository.findById(firstClaims.tokenId()))
                .isPresent()
                .get()
                .extracting(RefreshToken::revocationReason)
                .isEqualTo(SessionRevocationReason.REPLACED_BY_NEW_LOGIN);
        assertThat(authSessionRepository.findActiveByUserId(userAccount.id())).hasSize(1);
    }

    @Test
    void refreshRotatesRefreshTokenForActiveSession() {
        InMemoryAuthSessionRepository authSessionRepository = new InMemoryAuthSessionRepository();
        InMemoryRefreshTokenRepository refreshTokenRepository = new InMemoryRefreshTokenRepository();
        InMemoryUserAccountRepository userAccountRepository = new InMemoryUserAccountRepository();
        InMemoryAuthTokenCodec authTokenCodec = new InMemoryAuthTokenCodec();
        AuthSessionService service = new AuthSessionService(
                authSessionRepository,
                refreshTokenRepository,
                userAccountRepository,
                new LoginUserPolicy(),
                authTokenCodec,
                authTokenCodec,
                FIXED_CLOCK
        );
        UserAccount userAccount = activeUser();
        userAccountRepository.save(userAccount);

        AuthTokenPair initialPair = service.startSession(userAccount);
        AuthTokenClaims initialClaims = authTokenCodec.verify(
                initialPair.refreshToken(),
                AuthTokenType.REFRESH,
                OffsetDateTime.now(FIXED_CLOCK)
        );

        AuthTokenPair refreshedPair = service.refresh(initialPair.refreshToken());
        AuthTokenClaims refreshedClaims = authTokenCodec.verify(
                refreshedPair.refreshToken(),
                AuthTokenType.REFRESH,
                OffsetDateTime.now(FIXED_CLOCK)
        );

        assertThat(refreshedClaims.sessionId()).isEqualTo(initialClaims.sessionId());
        assertThat(refreshedClaims.tokenId()).isNotEqualTo(initialClaims.tokenId());
        assertThat(refreshTokenRepository.findById(initialClaims.tokenId()))
                .isPresent()
                .get()
                .satisfies(refreshToken -> {
                    assertThat(refreshToken.consumedAt()).isEqualTo(OffsetDateTime.now(FIXED_CLOCK));
                    assertThat(refreshToken.replacedByTokenId()).isEqualTo(refreshedClaims.tokenId());
                });
    }

    @Test
    void logoutRevokesSessionAndRefreshTokens() {
        InMemoryAuthSessionRepository authSessionRepository = new InMemoryAuthSessionRepository();
        InMemoryRefreshTokenRepository refreshTokenRepository = new InMemoryRefreshTokenRepository();
        InMemoryUserAccountRepository userAccountRepository = new InMemoryUserAccountRepository();
        InMemoryAuthTokenCodec authTokenCodec = new InMemoryAuthTokenCodec();
        AuthSessionService service = new AuthSessionService(
                authSessionRepository,
                refreshTokenRepository,
                userAccountRepository,
                new LoginUserPolicy(),
                authTokenCodec,
                authTokenCodec,
                FIXED_CLOCK
        );
        UserAccount userAccount = activeUser();
        userAccountRepository.save(userAccount);

        AuthTokenPair tokenPair = service.startSession(userAccount);
        AuthTokenClaims claims = authTokenCodec.verify(
                tokenPair.refreshToken(),
                AuthTokenType.REFRESH,
                OffsetDateTime.now(FIXED_CLOCK)
        );

        service.logout(tokenPair.refreshToken());

        assertThat(authSessionRepository.findById(claims.sessionId()))
                .isPresent()
                .get()
                .extracting(AuthSession::revocationReason)
                .isEqualTo(SessionRevocationReason.LOGGED_OUT);
        assertThat(refreshTokenRepository.findById(claims.tokenId()))
                .isPresent()
                .get()
                .extracting(RefreshToken::revocationReason)
                .isEqualTo(SessionRevocationReason.LOGGED_OUT);
        assertThatThrownBy(() -> service.refresh(tokenPair.refreshToken()))
                .isInstanceOf(RevokedRefreshTokenException.class);
    }

    @Test
    void refreshRejectsPreviousSessionAfterReplacement() {
        InMemoryAuthSessionRepository authSessionRepository = new InMemoryAuthSessionRepository();
        InMemoryRefreshTokenRepository refreshTokenRepository = new InMemoryRefreshTokenRepository();
        InMemoryUserAccountRepository userAccountRepository = new InMemoryUserAccountRepository();
        InMemoryAuthTokenCodec authTokenCodec = new InMemoryAuthTokenCodec();
        AuthSessionService service = new AuthSessionService(
                authSessionRepository,
                refreshTokenRepository,
                userAccountRepository,
                new LoginUserPolicy(),
                authTokenCodec,
                authTokenCodec,
                FIXED_CLOCK
        );
        UserAccount userAccount = activeUser();
        userAccountRepository.save(userAccount);

        AuthTokenPair firstPair = service.startSession(userAccount);
        service.startSession(userAccount);

        assertThatThrownBy(() -> service.refresh(firstPair.refreshToken()))
                .isInstanceOf(SessionReplacedException.class);
    }

    private static UserAccount activeUser() {
        return TestUserAccounts.userAccount(
                "5511999990001",
                "Admin User",
                UserRole.ADMIN,
                true,
                OffsetDateTime.now(FIXED_CLOCK)
        );
    }

    private static final class InMemoryAuthSessionRepository implements AuthSessionRepository {

        private final Map<UUID, AuthSession> sessionsById = new HashMap<>();

        @Override
        public AuthSession save(AuthSession session) {
            UUID sessionId = session.id() == null ? UUID.randomUUID() : session.id();
            AuthSession persisted = new AuthSession(
                    sessionId,
                    session.userId(),
                    session.farmId(),
                    session.revokedAt(),
                    session.revocationReason(),
                    session.createdAt(),
                    session.updatedAt()
            );
            sessionsById.put(sessionId, persisted);
            return persisted;
        }

        @Override
        public List<AuthSession> findActiveByUserId(UUID userId) {
            return sessionsById.values().stream()
                    .filter(session -> session.userId().equals(userId))
                    .filter(AuthSession::isActive)
                    .toList();
        }

        @Override
        public Optional<AuthSession> findById(UUID sessionId) {
            return Optional.ofNullable(sessionsById.get(sessionId));
        }
    }

    private static final class InMemoryRefreshTokenRepository implements RefreshTokenRepository {

        private final Map<UUID, RefreshToken> refreshTokensById = new HashMap<>();

        @Override
        public RefreshToken save(RefreshToken refreshToken) {
            refreshTokensById.put(refreshToken.id(), refreshToken);
            return refreshToken;
        }

        @Override
        public Optional<RefreshToken> findById(UUID tokenId) {
            return Optional.ofNullable(refreshTokensById.get(tokenId));
        }

        @Override
        public List<RefreshToken> findAllBySessionIdAndNotRevoked(UUID sessionId) {
            return refreshTokensById.values().stream()
                    .filter(refreshToken -> refreshToken.sessionId().equals(sessionId))
                    .filter(refreshToken -> !refreshToken.isRevoked())
                    .toList();
        }
    }

    private static final class InMemoryAuthTokenCodec implements AuthTokenPairIssuer, AuthTokenParser {

        @Override
        public AuthTokenPair issueFor(
                UserAccount userAccount,
                UUID sessionId,
                UUID refreshTokenId,
                OffsetDateTime issuedAt,
                OffsetDateTime refreshTokenExpiresAt
        ) {
            return new AuthTokenPair(
                    encodeToken(userAccount, sessionId, UUID.randomUUID(), AuthTokenType.ACCESS, issuedAt, issuedAt.plusMinutes(15)),
                    encodeToken(userAccount, sessionId, refreshTokenId, AuthTokenType.REFRESH, issuedAt, refreshTokenExpiresAt),
                    "Bearer",
                    issuedAt.plusMinutes(15),
                    refreshTokenExpiresAt
            );
        }

        @Override
        public AuthTokenClaims verify(String token, AuthTokenType expectedType, OffsetDateTime now) {
            String[] parts = token.split("\\|");
            if (parts.length != 8) {
                throw new InvalidAuthTokenException();
            }

            AuthTokenType tokenType = AuthTokenType.fromClaimValue(parts[0]);
            if (tokenType != expectedType) {
                throw new InvalidAuthTokenException();
            }

            OffsetDateTime expiresAt = OffsetDateTime.ofInstant(Instant.ofEpochSecond(Long.parseLong(parts[7])), ZoneOffset.UTC);
            if (expiresAt.isBefore(now)) {
                throw new ExpiredAuthTokenException();
            }

            return new AuthTokenClaims(
                    UUID.fromString(parts[1]),
                    UUID.fromString(parts[2]),
                    UserRole.fromDatabaseValue(parts[3]),
                    UUID.fromString(parts[4]),
                    UUID.fromString(parts[5]),
                    tokenType,
                    OffsetDateTime.ofInstant(Instant.ofEpochSecond(Long.parseLong(parts[6])), ZoneOffset.UTC),
                    expiresAt
            );
        }

        private String encodeToken(
                UserAccount userAccount,
                UUID sessionId,
                UUID tokenId,
                AuthTokenType tokenType,
                OffsetDateTime issuedAt,
                OffsetDateTime expiresAt
        ) {
            return String.join(
                    "|",
                    tokenType.claimValue(),
                    userAccount.id().toString(),
                    userAccount.farmId().toString(),
                    userAccount.role().name().toLowerCase(),
                    sessionId.toString(),
                    tokenId.toString(),
                    String.valueOf(issuedAt.toEpochSecond()),
                    String.valueOf(expiresAt.toEpochSecond())
            );
        }
    }
}
