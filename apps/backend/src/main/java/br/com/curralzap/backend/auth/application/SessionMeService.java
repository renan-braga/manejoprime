package br.com.curralzap.backend.auth.application;

import br.com.curralzap.backend.billing.application.BillingAccessPolicy;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserAccountRepository;
import org.springframework.stereotype.Service;

@Service
public class SessionMeService {

    private final UserAccountRepository userAccountRepository;
    private final SessionMeViewRepository sessionMeViewRepository;
    private final BillingAccessPolicy billingAccessPolicy;

    SessionMeService(
            UserAccountRepository userAccountRepository,
            SessionMeViewRepository sessionMeViewRepository,
            BillingAccessPolicy billingAccessPolicy
    ) {
        this.userAccountRepository = userAccountRepository;
        this.sessionMeViewRepository = sessionMeViewRepository;
        this.billingAccessPolicy = billingAccessPolicy;
    }

    public SessionMeView getSession(AuthTokenClaims claims) {
        UserAccount userAccount = userAccountRepository.findById(claims.userId())
                .orElseThrow(AuthenticationRequiredException::new);
        if (!userAccount.active()) {
            throw new InactiveUserException();
        }

        // farmId comes from the JWT claim so session/me reflects the active farm
        // regardless of users.farm_id (which is the primary/default farm, never changed on switch).
        SessionMeView session = sessionMeViewRepository.findByUserIdAndFarmId(userAccount.id(), claims.farmId())
                .orElseThrow(AuthenticationRequiredException::new);
        return session.withBilling(billingAccessPolicy.snapshotFor(claims.farmId()));
    }
}
