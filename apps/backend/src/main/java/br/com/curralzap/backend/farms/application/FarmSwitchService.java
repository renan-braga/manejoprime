package br.com.curralzap.backend.farms.application;

import br.com.curralzap.backend.auth.application.AuthSessionLifecycle;
import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.auth.application.AuthTokenPair;
import br.com.curralzap.backend.auth.application.AuthenticationRequiredException;
import br.com.curralzap.backend.farms.domain.UserFarmMembershipRepository;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserAccountRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class FarmSwitchService {

    private final UserAccountRepository userAccountRepository;
    private final UserFarmMembershipRepository membershipRepository;
    private final AuthSessionLifecycle authSessionLifecycle;

    public FarmSwitchService(
            UserAccountRepository userAccountRepository,
            UserFarmMembershipRepository membershipRepository,
            AuthSessionLifecycle authSessionLifecycle
    ) {
        this.userAccountRepository = userAccountRepository;
        this.membershipRepository = membershipRepository;
        this.authSessionLifecycle = authSessionLifecycle;
    }

    @Transactional
    public AuthTokenPair switchFarm(AuthTokenClaims claims, UUID targetFarmId) {
        if (!membershipRepository.existsByUserIdAndFarmId(claims.userId(), targetFarmId)) {
            throw new FarmSwitchForbiddenException();
        }

        UserAccount user = userAccountRepository.findById(claims.userId())
                .orElseThrow(AuthenticationRequiredException::new);

        // users.farm_id is the primary/default farm and must not be updated here:
        // many data tables (whatsapp_inbound_messages, weighings, medications, etc.)
        // hold composite FKs (farm_id, user_id) → users(farm_id, id) without CASCADE,
        // so updating users.farm_id would be rejected by PostgreSQL.
        // auth_sessions now carries only simple FKs to users(id) and farms(id),
        // so startSession works regardless of users.farm_id.
        return authSessionLifecycle.startSession(user.withFarmId(targetFarmId));
    }
}
