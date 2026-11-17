package br.com.curralzap.backend.animals.application;

import br.com.curralzap.backend.animals.domain.Animal;
import br.com.curralzap.backend.animals.domain.AnimalRepository;
import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.auth.application.AuthenticationRequiredException;
import br.com.curralzap.backend.auth.application.LoginUserPolicy;
import br.com.curralzap.backend.auth.application.RolePermissionSummary;
import br.com.curralzap.backend.shared.application.FarmScopedAccessGuard;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserAccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class AnimalWriteService {

    private final UserAccountRepository userAccountRepository;
    private final LoginUserPolicy loginUserPolicy;
    private final AnimalRepository animalRepository;
    private final FarmScopedAccessGuard farmScopedAccessGuard;

    public AnimalWriteService(
            UserAccountRepository userAccountRepository,
            LoginUserPolicy loginUserPolicy,
            AnimalRepository animalRepository,
            FarmScopedAccessGuard farmScopedAccessGuard
    ) {
        this.userAccountRepository = userAccountRepository;
        this.loginUserPolicy = loginUserPolicy;
        this.animalRepository = animalRepository;
        this.farmScopedAccessGuard = farmScopedAccessGuard;
    }

    public AnimalMutationResult create(AuthTokenClaims claims, AnimalMutationCommand command) {
        requireWritableUser(claims);
        validateIdentifierUniqueness(claims.farmId(), command.identifier(), null);
        Animal createdAnimal = animalRepository.save(Animal.create(claims.farmId(), command));
        return new AnimalMutationResult(createdAnimal.id());
    }

    public void delete(AuthTokenClaims claims, UUID animalId) {
        requireWritableUser(claims);
        farmScopedAccessGuard.requireInFarm(
                animalRepository,
                animalId,
                claims.farmId(),
                AnimalNotFoundException::new
        );
        try {
            animalRepository.delete(animalId);
        } catch (DataIntegrityViolationException e) {
            throw new AnimalHasRecordsException();
        }
    }

    public AnimalMutationResult update(AuthTokenClaims claims, UUID animalId, AnimalMutationCommand command) {
        requireWritableUser(claims);
        Animal existingAnimal = farmScopedAccessGuard.requireInFarm(
                animalRepository,
                animalId,
                claims.farmId(),
                AnimalNotFoundException::new
        );
        validateIdentifierUniqueness(claims.farmId(), command.identifier(), animalId);
        Animal updatedAnimal = animalRepository.save(existingAnimal.update(command));
        return new AnimalMutationResult(updatedAnimal.id());
    }

    private UserAccount requireWritableUser(AuthTokenClaims claims) {
        UserAccount userAccount = userAccountRepository.findById(claims.userId())
                .orElseThrow(AuthenticationRequiredException::new);
        UserAccount activeUser = loginUserPolicy.requireActive(userAccount);
        if (!RolePermissionSummary.permissionsFor(activeUser.role()).contains("animals.write")) {
            throw new AnimalWriteForbiddenException();
        }
        return activeUser;
    }

    private void validateIdentifierUniqueness(UUID farmId, String identifier, UUID animalId) {
        boolean duplicated = animalId == null
                ? animalRepository.existsByFarmIdAndIdentifier(farmId, identifier)
                : animalRepository.existsByFarmIdAndIdentifierAndIdNot(farmId, identifier, animalId);

        if (duplicated) {
            throw new InvalidAnimalMutationException(
                    Map.of("identifier", "Identifier must be unique within the farm.")
            );
        }
    }
}
