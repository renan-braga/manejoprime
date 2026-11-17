package br.com.curralzap.backend.auth.application;

import br.com.curralzap.backend.auth.domain.AuthSession;
import br.com.curralzap.backend.auth.domain.RefreshToken;
import br.com.curralzap.backend.auth.domain.SessionRevocationReason;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserAccountRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class AuthSessionService implements AuthSessionLifecycle {

    private static final long REFRESH_TOKEN_TTL_DAYS = 30L;

    private final AuthSessionRepository authSessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserAccountRepository userAccountRepository;
    private final LoginUserPolicy loginUserPolicy;
    private final AuthTokenPairIssuer authTokenPairIssuer;
    private final AuthTokenParser authTokenParser;
    private final Clock clock;

    @Autowired
    public AuthSessionService(
            AuthSessionRepository authSessionRepository,
            RefreshTokenRepository refreshTokenRepository,
            UserAccountRepository userAccountRepository,
            LoginUserPolicy loginUserPolicy,
            AuthTokenPairIssuer authTokenPairIssuer,
            AuthTokenParser authTokenParser
    ) {
        this(
                authSessionRepository,
                refreshTokenRepository,
                userAccountRepository,
                loginUserPolicy,
                authTokenPairIssuer,
                authTokenParser,
                Clock.systemUTC()
        );
    }

    AuthSessionService(
            AuthSessionRepository authSessionRepository,
            RefreshTokenRepository refreshTokenRepository,
            UserAccountRepository userAccountRepository,
            LoginUserPolicy loginUserPolicy,
            AuthTokenPairIssuer authTokenPairIssuer,
            AuthTokenParser authTokenParser,
            Clock clock
    ) {
        this.authSessionRepository = authSessionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userAccountRepository = userAccountRepository;
        this.loginUserPolicy = loginUserPolicy;
        this.authTokenPairIssuer = authTokenPairIssuer;
        this.authTokenParser = authTokenParser;
        this.clock = clock;
    }

    @Override
    @Transactional
    public AuthTokenPair startSession(UserAccount userAccount) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        revokeActiveSessions(userAccount.id(), now, SessionRevocationReason.REPLACED_BY_NEW_LOGIN);

        AuthSession session = authSessionRepository.save(AuthSession.start(userAccount.id(), userAccount.farmId(), now));
        RefreshToken refreshToken = refreshTokenRepository.save(RefreshToken.issue(
                UUID.randomUUID(),
                session.id(),
                userAccount.id(),
                userAccount.farmId(),
                now,
                now.plusDays(REFRESH_TOKEN_TTL_DAYS)
        ));

        return authTokenPairIssuer.issueFor(userAccount, session.id(), refreshToken.id(), now, refreshToken.expiresAt());
    }

    @Override
    @Transactional
    public AuthTokenPair refresh(String refreshToken) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        AuthTokenClaims claims = parseRefreshToken(refreshToken, now);

        AuthSession session = authSessionRepository.findById(claims.sessionId())
                .orElseThrow(InvalidRefreshTokenException::new);
        RefreshToken persistedRefreshToken = refreshTokenRepository.findById(claims.tokenId())
                .orElseThrow(InvalidRefreshTokenException::new);

        validateRefreshTokenState(claims, session, persistedRefreshToken, now);

        UserAccount userAccount = userAccountRepository.findById(claims.userId())
                .orElseThrow(InvalidRefreshTokenException::new);
        loginUserPolicy.requireActive(userAccount);

        RefreshToken nextRefreshToken = RefreshToken.issue(
                UUID.randomUUID(),
                session.id(),
                userAccount.id(),
                userAccount.farmId(),
                now,
                now.plusDays(REFRESH_TOKEN_TTL_DAYS)
        );
        refreshTokenRepository.save(persistedRefreshToken.markConsumed(now, nextRefreshToken.id()));
        refreshTokenRepository.save(nextRefreshToken);

        return authTokenPairIssuer.issueFor(userAccount, session.id(), nextRefreshToken.id(), now, nextRefreshToken.expiresAt());
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        AuthTokenClaims claims = parseRefreshToken(refreshToken, now);

        AuthSession session = authSessionRepository.findById(claims.sessionId())
                .orElseThrow(InvalidRefreshTokenException::new);
        RefreshToken persistedRefreshToken = refreshTokenRepository.findById(claims.tokenId())
                .orElseThrow(InvalidRefreshTokenException::new);

        validateRefreshTokenState(claims, session, persistedRefreshToken, now);

        authSessionRepository.save(session.revoke(SessionRevocationReason.LOGGED_OUT, now));
        revokeRefreshTokensForSession(session.id(), SessionRevocationReason.LOGGED_OUT, now);
    }

    private AuthTokenClaims parseRefreshToken(String refreshToken, OffsetDateTime now) {
        try {
            return authTokenParser.verify(refreshToken, AuthTokenType.REFRESH, now);
        } catch (ExpiredAuthTokenException exception) {
            throw new ExpiredRefreshTokenException();
        } catch (InvalidAuthTokenException exception) {
            throw new InvalidRefreshTokenException();
        }
    }

    private void validateRefreshTokenState(
            AuthTokenClaims claims,
            AuthSession session,
            RefreshToken persistedRefreshToken,
            OffsetDateTime now
    ) {
        if (!persistedRefreshToken.sessionId().equals(claims.sessionId())
                || !persistedRefreshToken.userId().equals(claims.userId())
                || !persistedRefreshToken.farmId().equals(claims.farmId())
                || !persistedRefreshToken.id().equals(claims.tokenId())) {
            throw new InvalidRefreshTokenException();
        }

        if (persistedRefreshToken.isExpiredAt(now)) {
            throw new ExpiredRefreshTokenException();
        }

        if (!session.isActive()) {
            if (session.isReplacedByNewLogin()) {
                throw new SessionReplacedException();
            }
            throw new RevokedRefreshTokenException();
        }

        if (persistedRefreshToken.isRevoked()) {
            if (persistedRefreshToken.isReplacedByNewLogin()) {
                throw new SessionReplacedException();
            }
            throw new RevokedRefreshTokenException();
        }

        if (persistedRefreshToken.isConsumed()) {
            throw new RevokedRefreshTokenException();
        }
    }

    private void revokeActiveSessions(UUID userId, OffsetDateTime now, SessionRevocationReason reason) {
        for (AuthSession existingSession : authSessionRepository.findActiveByUserId(userId)) {
            authSessionRepository.save(existingSession.revoke(reason, now));
            revokeRefreshTokensForSession(existingSession.id(), reason, now);
        }
    }

    private void revokeRefreshTokensForSession(UUID sessionId, SessionRevocationReason reason, OffsetDateTime now) {
        for (RefreshToken refreshToken : refreshTokenRepository.findAllBySessionIdAndNotRevoked(sessionId)) {
            refreshTokenRepository.save(refreshToken.revoke(reason, now));
        }
    }
}
