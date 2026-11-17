package br.com.curralzap.backend.auth.application;

import br.com.curralzap.backend.users.domain.Invitation;
import br.com.curralzap.backend.users.domain.InvitationRepository;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserAccountRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Service
public class OAuthCallbackService {

    private final List<OAuthCodeExchanger> codeExchangers;
    private final List<OAuthTokenVerifier> verifiers;
    private final UserAccountRepository userAccountRepository;
    private final InvitationRepository invitationRepository;
    private final AuthSessionLifecycle authSessionLifecycle;
    private final LoginUserPolicy loginUserPolicy;
    private final OnboardingTokenIssuer onboardingTokenIssuer;
    private final Duration onboardingTokenTtl;

    public OAuthCallbackService(
            List<OAuthCodeExchanger> codeExchangers,
            List<OAuthTokenVerifier> verifiers,
            UserAccountRepository userAccountRepository,
            InvitationRepository invitationRepository,
            AuthSessionLifecycle authSessionLifecycle,
            LoginUserPolicy loginUserPolicy,
            OnboardingTokenIssuer onboardingTokenIssuer,
            @Value("${curralzap.auth.onboarding-token-ttl:PT15M}") Duration onboardingTokenTtl
    ) {
        this.codeExchangers = codeExchangers;
        this.verifiers = verifiers;
        this.userAccountRepository = userAccountRepository;
        this.invitationRepository = invitationRepository;
        this.authSessionLifecycle = authSessionLifecycle;
        this.loginUserPolicy = loginUserPolicy;
        this.onboardingTokenIssuer = onboardingTokenIssuer;
        this.onboardingTokenTtl = onboardingTokenTtl;
    }

    @Transactional
    public OAuthCallbackResult authenticate(String provider, String code, String codeVerifier, String redirectUri) {
        OAuthCodeExchanger exchanger = codeExchangers.stream()
                .filter(e -> e.supports(provider))
                .findFirst()
                .orElseThrow(() -> new OAuthTokenVerificationException("Unsupported OAuth provider: " + provider));

        String idToken = exchanger.exchangeForIdToken(code, codeVerifier, redirectUri);

        OAuthTokenVerifier verifier = verifiers.stream()
                .filter(v -> v.supports(provider))
                .findFirst()
                .orElseThrow(() -> new OAuthTokenVerificationException("Unsupported OAuth provider: " + provider));

        OAuthIdentity identity = verifier.verify(idToken);

        Optional<UserAccount> existingUser = userAccountRepository.findByEmail(identity.email());
        if (existingUser.isPresent()) {
            UserAccount user = loginUserPolicy.requireActive(existingUser.get());
            return new OAuthCallbackResult.Authenticated(authSessionLifecycle.startSession(user));
        }

        Optional<Invitation> invitation = invitationRepository.findPendingByEmail(identity.email());
        if (invitation.isPresent()) {
            UserAccount newUser = userAccountRepository.save(
                    UserAccount.create(invitation.get().farmId(), invitation.get().phone(), identity.email(), identity.name(), invitation.get().role())
            );
            invitationRepository.save(invitation.get().accept());
            return new OAuthCallbackResult.Authenticated(authSessionLifecycle.startSession(newUser));
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String token = onboardingTokenIssuer.issue(identity, now, now.plus(onboardingTokenTtl));
        return new OAuthCallbackResult.OnboardingRequired(token);
    }
}
