package br.com.curralzap.backend.weighings.application;

import br.com.curralzap.backend.animals.domain.AnimalRepository;
import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.auth.application.AuthenticationRequiredException;
import br.com.curralzap.backend.auth.application.LoginUserPolicy;
import br.com.curralzap.backend.auth.application.RolePermissionSummary;
import br.com.curralzap.backend.shared.application.FarmScopedAccessGuard;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserAccountRepository;
import br.com.curralzap.backend.weighings.domain.Weighing;
import br.com.curralzap.backend.weighings.domain.WeighingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class WeighingWriteService {

    private final UserAccountRepository userAccountRepository;
    private final LoginUserPolicy loginUserPolicy;
    private final AnimalRepository animalRepository;
    private final WeighingRepository weighingRepository;
    private final FarmScopedAccessGuard farmScopedAccessGuard;

    WeighingWriteService(
            UserAccountRepository userAccountRepository,
            LoginUserPolicy loginUserPolicy,
            AnimalRepository animalRepository,
            WeighingRepository weighingRepository,
            FarmScopedAccessGuard farmScopedAccessGuard
    ) {
        this.userAccountRepository = userAccountRepository;
        this.loginUserPolicy = loginUserPolicy;
        this.animalRepository = animalRepository;
        this.weighingRepository = weighingRepository;
        this.farmScopedAccessGuard = farmScopedAccessGuard;
    }

    @Transactional
    public WeighingCreateResult create(
            AuthTokenClaims claims,
            WeighingCreateCommand command,
            String idempotencyKey
    ) {
        UserAccount activeUser = requireWritableUser(claims);

        if (idempotencyKey != null) {
            Weighing existingWeighing = weighingRepository.findByFarmIdAndIdempotencyKey(claims.farmId(), idempotencyKey)
                    .orElse(null);
            if (existingWeighing != null) {
                return toResult(existingWeighing);
            }
        }

        farmScopedAccessGuard.requireInFarm(
                animalRepository,
                command.animalId(),
                claims.farmId(),
                WeighingAnimalNotFoundException::new
        );

        Weighing savedWeighing = weighingRepository.save(
                Weighing.create(claims.farmId(), activeUser.id(), command, idempotencyKey)
        );
        return toResult(savedWeighing);
    }

    private UserAccount requireWritableUser(AuthTokenClaims claims) {
        UserAccount userAccount = userAccountRepository.findById(claims.userId())
                .orElseThrow(AuthenticationRequiredException::new);
        UserAccount activeUser = loginUserPolicy.requireActive(userAccount);
        if (!RolePermissionSummary.permissionsFor(activeUser.role()).contains("weighings.write")) {
            throw new WeighingWriteForbiddenException();
        }
        return activeUser;
    }

    private WeighingCreateResult toResult(Weighing weighing) {
        return new WeighingCreateResult(
                weighing.id(),
                weighing.animalId(),
                weighing.weightKg(),
                weighing.weighingDate(),
                weighing.type().apiValue(),
                weighing.source().apiValue(),
                weighing.recordedBy(),
                calculateGmd(weighing)
        );
    }

    private BigDecimal calculateGmd(Weighing weighing) {
        return weighingRepository.findLatestBefore(weighing.farmId(), weighing.animalId(), weighing.weighingDate())
                .map(previousWeighing -> {
                    long daysBetween = ChronoUnit.DAYS.between(previousWeighing.weighingDate(), weighing.weighingDate());
                    if (daysBetween <= 0) {
                        return null;
                    }

                    return weighing.weightKg()
                            .subtract(previousWeighing.weightKg())
                            .divide(BigDecimal.valueOf(daysBetween), 2, RoundingMode.HALF_UP);
                })
                .orElse(null);
    }
}
