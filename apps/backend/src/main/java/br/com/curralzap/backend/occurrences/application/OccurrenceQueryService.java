package br.com.curralzap.backend.occurrences.application;

import br.com.curralzap.backend.animals.application.AnimalNotFoundException;
import br.com.curralzap.backend.animals.domain.AnimalRepository;
import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.auth.application.AuthenticationRequiredException;
import br.com.curralzap.backend.auth.application.LoginUserPolicy;
import br.com.curralzap.backend.auth.application.RolePermissionSummary;
import br.com.curralzap.backend.shared.application.FarmScopedAccessGuard;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OccurrenceQueryService {

    private final UserAccountRepository userAccountRepository;
    private final LoginUserPolicy loginUserPolicy;
    private final AnimalRepository animalRepository;
    private final OccurrenceReadViewRepository occurrenceReadViewRepository;
    private final FarmScopedAccessGuard farmScopedAccessGuard;

    OccurrenceQueryService(
            UserAccountRepository userAccountRepository,
            LoginUserPolicy loginUserPolicy,
            AnimalRepository animalRepository,
            OccurrenceReadViewRepository occurrenceReadViewRepository,
            FarmScopedAccessGuard farmScopedAccessGuard
    ) {
        this.userAccountRepository = userAccountRepository;
        this.loginUserPolicy = loginUserPolicy;
        this.animalRepository = animalRepository;
        this.occurrenceReadViewRepository = occurrenceReadViewRepository;
        this.farmScopedAccessGuard = farmScopedAccessGuard;
    }

    @Transactional(readOnly = true)
    public OccurrenceListView list(AuthTokenClaims claims, OccurrenceListQuery query) {
        requireOccurrenceAccess(claims);

        if (query.animalId() != null) {
            farmScopedAccessGuard.requireInFarm(
                    animalRepository,
                    query.animalId(),
                    claims.farmId(),
                    AnimalNotFoundException::new
            );
        }

        return occurrenceReadViewRepository.listFor(claims.farmId(), query);
    }

    private void requireOccurrenceAccess(AuthTokenClaims claims) {
        UserAccount userAccount = userAccountRepository.findById(claims.userId())
                .orElseThrow(AuthenticationRequiredException::new);
        UserAccount activeUser = loginUserPolicy.requireActive(userAccount);
        if (!RolePermissionSummary.permissionsFor(activeUser.role()).contains("occurrences.write")) {
            throw new OccurrenceQueryForbiddenException();
        }
    }
}
