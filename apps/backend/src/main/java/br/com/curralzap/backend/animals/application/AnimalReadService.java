package br.com.curralzap.backend.animals.application;

import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.auth.application.AuthenticationRequiredException;
import br.com.curralzap.backend.auth.application.LoginUserPolicy;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserAccountRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AnimalReadService {

    private final UserAccountRepository userAccountRepository;
    private final LoginUserPolicy loginUserPolicy;
    private final AnimalReadViewRepository animalReadViewRepository;

    public AnimalReadService(
            UserAccountRepository userAccountRepository,
            LoginUserPolicy loginUserPolicy,
            AnimalReadViewRepository animalReadViewRepository
    ) {
        this.userAccountRepository = userAccountRepository;
        this.loginUserPolicy = loginUserPolicy;
        this.animalReadViewRepository = animalReadViewRepository;
    }

    public AnimalListView list(AuthTokenClaims claims, AnimalListQuery query) {
        requireActiveUser(claims);
        return animalReadViewRepository.listFor(claims.farmId(), query);
    }

    public AnimalDetailView detail(AuthTokenClaims claims, UUID animalId) {
        requireActiveUser(claims);
        return animalReadViewRepository.findDetailById(claims.farmId(), animalId)
                .orElseThrow(AnimalNotFoundException::new);
    }

    private UserAccount requireActiveUser(AuthTokenClaims claims) {
        UserAccount userAccount = userAccountRepository.findById(claims.userId())
                .orElseThrow(AuthenticationRequiredException::new);
        return loginUserPolicy.requireActive(userAccount);
    }
}
