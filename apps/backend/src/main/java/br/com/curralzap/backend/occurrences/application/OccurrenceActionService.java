package br.com.curralzap.backend.occurrences.application;

import br.com.curralzap.backend.animals.application.AnimalNotFoundException;
import br.com.curralzap.backend.animals.domain.AnimalRepository;
import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.auth.application.AuthenticationRequiredException;
import br.com.curralzap.backend.auth.application.LoginUserPolicy;
import br.com.curralzap.backend.auth.application.RolePermissionSummary;
import br.com.curralzap.backend.occurrences.domain.Occurrence;
import br.com.curralzap.backend.occurrences.domain.OccurrenceRepository;
import br.com.curralzap.backend.occurrences.domain.OccurrenceStatus;
import br.com.curralzap.backend.shared.application.FarmScopedAccessGuard;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserAccountRepository;
import br.com.curralzap.backend.users.domain.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class OccurrenceActionService {

    private final UserAccountRepository userAccountRepository;
    private final LoginUserPolicy loginUserPolicy;
    private final AnimalRepository animalRepository;
    private final OccurrenceRepository occurrenceRepository;
    private final FarmScopedAccessGuard farmScopedAccessGuard;

    OccurrenceActionService(
            UserAccountRepository userAccountRepository,
            LoginUserPolicy loginUserPolicy,
            AnimalRepository animalRepository,
            OccurrenceRepository occurrenceRepository,
            FarmScopedAccessGuard farmScopedAccessGuard
    ) {
        this.userAccountRepository = userAccountRepository;
        this.loginUserPolicy = loginUserPolicy;
        this.animalRepository = animalRepository;
        this.occurrenceRepository = occurrenceRepository;
        this.farmScopedAccessGuard = farmScopedAccessGuard;
    }

    @Transactional
    public OccurrenceView register(AuthTokenClaims claims, OccurrenceCreateCommand command) {
        UserAccount user = userAccountRepository.findById(claims.userId())
                .orElseThrow(AuthenticationRequiredException::new);
        UserAccount activeUser = loginUserPolicy.requireActive(user);
        if (!RolePermissionSummary.permissionsFor(activeUser.role()).contains("occurrences.write")) {
            throw new OccurrenceActionForbiddenException();
        }
        farmScopedAccessGuard.requireInFarm(
                animalRepository,
                command.animalId(),
                claims.farmId(),
                AnimalNotFoundException::new
        );
        return OccurrenceView.from(occurrenceRepository.insert(
                Occurrence.register(claims.farmId(), command.animalId(), activeUser.id(), command.type(), command.severity(), command.description())
        ));
    }

    @Transactional
    public OccurrenceActionResult resolve(AuthTokenClaims claims, UUID occurrenceId) {
        UserAccount activeUser = requireAuthorizedUser(claims);
        Occurrence updated = occurrenceRepository.save(
                findOccurrence(claims, occurrenceId).resolve(activeUser.id(), OffsetDateTime.now(ZoneOffset.UTC))
        );
        return toResult(updated);
    }

    @Transactional
    public OccurrenceActionResult reopen(AuthTokenClaims claims, UUID occurrenceId) {
        requireAuthorizedUser(claims);
        Occurrence updated = occurrenceRepository.save(
                findOccurrence(claims, occurrenceId).reopen(OffsetDateTime.now(ZoneOffset.UTC))
        );
        return toResult(updated);
    }

    @Transactional
    public void delete(AuthTokenClaims claims, UUID occurrenceId) {
        requireAuthorizedUser(claims);
        Occurrence occurrence = findOccurrence(claims, occurrenceId);
        occurrenceRepository.delete(occurrence.id());
    }

    private UserAccount requireAuthorizedUser(AuthTokenClaims claims) {
        UserAccount user = userAccountRepository.findById(claims.userId())
                .orElseThrow(AuthenticationRequiredException::new);
        UserAccount activeUser = loginUserPolicy.requireActive(user);
        if (activeUser.role() == UserRole.WORKER) {
            throw new OccurrenceActionForbiddenException();
        }
        return activeUser;
    }

    private Occurrence findOccurrence(AuthTokenClaims claims, UUID occurrenceId) {
        return farmScopedAccessGuard.requireInFarm(
                occurrenceRepository,
                occurrenceId,
                claims.farmId(),
                OccurrenceNotFoundException::new
        );
    }

    private OccurrenceActionResult toResult(Occurrence occurrence) {
        return new OccurrenceActionResult(
                occurrence.id(),
                occurrence.status().apiValue(),
                occurrence.resolvedBy(),
                occurrence.status() == OccurrenceStatus.RESOLVED ? occurrence.updatedAt() : null
        );
    }
}
