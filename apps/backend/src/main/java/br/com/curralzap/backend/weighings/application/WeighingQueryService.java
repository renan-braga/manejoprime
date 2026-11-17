package br.com.curralzap.backend.weighings.application;

import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.auth.application.AuthenticationRequiredException;
import br.com.curralzap.backend.auth.application.LoginUserPolicy;
import br.com.curralzap.backend.auth.application.RolePermissionSummary;
import br.com.curralzap.backend.animals.domain.AnimalRepository;
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
import java.util.Collections;
import java.util.List;

@Service
public class WeighingQueryService {

    private final UserAccountRepository userAccountRepository;
    private final LoginUserPolicy loginUserPolicy;
    private final AnimalRepository animalRepository;
    private final WeighingRepository weighingRepository;
    private final FarmScopedAccessGuard farmScopedAccessGuard;

    public WeighingQueryService(
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

    @Transactional(readOnly = true)
    public WeighingHistoryView list(AuthTokenClaims claims, WeighingListQuery query) {
        requireReadableUser(claims);

        farmScopedAccessGuard.requireInFarm(
                animalRepository,
                query.animalId(),
                claims.farmId(),
                WeighingAnimalNotFoundException::new
        );

        List<Weighing> history = weighingRepository.findHistory(claims.farmId(), query.animalId());
        int fromIndex = Math.min(query.offset(), history.size());
        int toIndex = Math.min(fromIndex + query.limit(), history.size());
        List<WeighingHistoryItemView> items = history.isEmpty()
                ? Collections.emptyList()
                : history.subList(fromIndex, toIndex).stream()
                        .map(weighing -> toHistoryItem(weighing, history))
                        .toList();

        return new WeighingHistoryView(items, history.size(), query.limit(), query.offset());
    }

    private void requireReadableUser(AuthTokenClaims claims) {
        UserAccount userAccount = userAccountRepository.findById(claims.userId())
                .orElseThrow(AuthenticationRequiredException::new);
        UserAccount activeUser = loginUserPolicy.requireActive(userAccount);
        if (!RolePermissionSummary.permissionsFor(activeUser.role()).contains("weighings.write")) {
            throw new WeighingWriteForbiddenException();
        }
    }

    private WeighingHistoryItemView toHistoryItem(Weighing weighing, List<Weighing> fullHistory) {
        return new WeighingHistoryItemView(
                weighing.id(),
                weighing.animalId(),
                weighing.weightKg(),
                weighing.weighingDate(),
                weighing.type().apiValue(),
                weighing.source().apiValue(),
                weighing.recordedBy(),
                calculateGmd(weighing, fullHistory)
        );
    }

    private BigDecimal calculateGmd(Weighing weighing, List<Weighing> fullHistory) {
        int currentIndex = fullHistory.indexOf(weighing);
        if (currentIndex < 0 || currentIndex + 1 >= fullHistory.size()) {
            return null;
        }

        Weighing previousWeighing = fullHistory.get(currentIndex + 1);
        long daysBetween = ChronoUnit.DAYS.between(previousWeighing.weighingDate(), weighing.weighingDate());
        if (daysBetween <= 0) {
            return null;
        }

        return weighing.weightKg()
                .subtract(previousWeighing.weightKg())
                .divide(BigDecimal.valueOf(daysBetween), 2, RoundingMode.HALF_UP);
    }
}
