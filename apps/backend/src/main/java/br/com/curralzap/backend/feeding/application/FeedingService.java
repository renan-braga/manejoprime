package br.com.curralzap.backend.feeding.application;

import br.com.curralzap.backend.animals.application.AnimalNotFoundException;
import br.com.curralzap.backend.animals.domain.AnimalRepository;
import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.auth.application.AuthenticationRequiredException;
import br.com.curralzap.backend.auth.application.LoginUserPolicy;
import br.com.curralzap.backend.auth.application.RolePermissionSummary;
import br.com.curralzap.backend.feeding.domain.Feeding;
import br.com.curralzap.backend.feeding.domain.FeedingRepository;
import br.com.curralzap.backend.feeding.domain.FeedingSource;
import br.com.curralzap.backend.lots.application.LotNotFoundException;
import br.com.curralzap.backend.lots.domain.LotRepository;
import br.com.curralzap.backend.shared.application.FarmScopedAccessGuard;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class FeedingService {

    private final UserAccountRepository userAccountRepository;
    private final LoginUserPolicy loginUserPolicy;
    private final AnimalRepository animalRepository;
    private final LotRepository lotRepository;
    private final FeedingRepository feedingRepository;
    private final FarmScopedAccessGuard farmScopedAccessGuard;

    public FeedingService(
            UserAccountRepository userAccountRepository,
            LoginUserPolicy loginUserPolicy,
            AnimalRepository animalRepository,
            LotRepository lotRepository,
            FeedingRepository feedingRepository,
            FarmScopedAccessGuard farmScopedAccessGuard
    ) {
        this.userAccountRepository = userAccountRepository;
        this.loginUserPolicy = loginUserPolicy;
        this.animalRepository = animalRepository;
        this.lotRepository = lotRepository;
        this.feedingRepository = feedingRepository;
        this.farmScopedAccessGuard = farmScopedAccessGuard;
    }

    @Transactional
    public FeedingView create(AuthTokenClaims claims, FeedingCreateCommand command) {
        UserAccount activeUser = requireFeedingAccess(claims);

        Feeding saved = command.animalTarget()
                ? createAnimalFeeding(claims, activeUser, command)
                : createLotFeeding(claims, activeUser, command);

        return toView(saved);
    }

    @Transactional(readOnly = true)
    public FeedingListView list(AuthTokenClaims claims, FeedingListQuery query) {
        requireFeedingAccess(claims);

        List<Feeding> history = query.animalTarget()
                ? listAnimalHistory(claims, query)
                : listLotHistory(claims, query);

        int fromIndex = Math.min(query.offset(), history.size());
        int toIndex = Math.min(fromIndex + query.limit(), history.size());
        List<FeedingView> items = history.isEmpty()
                ? Collections.emptyList()
                : history.subList(fromIndex, toIndex).stream().map(this::toView).toList();

        return new FeedingListView(items, history.size(), query.limit(), query.offset());
    }

    private Feeding createAnimalFeeding(AuthTokenClaims claims, UserAccount activeUser, FeedingCreateCommand command) {
        farmScopedAccessGuard.requireInFarm(
                animalRepository,
                command.animalId(),
                claims.farmId(),
                AnimalNotFoundException::new
        );
        return feedingRepository.save(
                Feeding.recordForAnimal(
                        claims.farmId(),
                        command.animalId(),
                        activeUser.id(),
                        command.feedType(),
                        command.description(),
                        command.quantityKg(),
                        command.costPerKg(),
                        command.startDate(),
                        command.endDate(),
                        FeedingSource.APP
                )
        );
    }

    private Feeding createLotFeeding(AuthTokenClaims claims, UserAccount activeUser, FeedingCreateCommand command) {
        farmScopedAccessGuard.requireInFarm(
                lotRepository,
                command.lotId(),
                claims.farmId(),
                LotNotFoundException::new
        );
        return feedingRepository.save(
                Feeding.recordForLot(
                        claims.farmId(),
                        command.lotId(),
                        activeUser.id(),
                        command.feedType(),
                        command.description(),
                        command.quantityKg(),
                        command.costPerKg(),
                        command.startDate(),
                        command.endDate(),
                        FeedingSource.APP
                )
        );
    }

    private List<Feeding> listAnimalHistory(AuthTokenClaims claims, FeedingListQuery query) {
        farmScopedAccessGuard.requireInFarm(
                animalRepository,
                query.animalId(),
                claims.farmId(),
                AnimalNotFoundException::new
        );
        return feedingRepository.findAnimalHistory(claims.farmId(), query.animalId());
    }

    private List<Feeding> listLotHistory(AuthTokenClaims claims, FeedingListQuery query) {
        farmScopedAccessGuard.requireInFarm(
                lotRepository,
                query.lotId(),
                claims.farmId(),
                LotNotFoundException::new
        );
        return feedingRepository.findLotHistory(claims.farmId(), query.lotId());
    }

    private UserAccount requireFeedingAccess(AuthTokenClaims claims) {
        UserAccount userAccount = userAccountRepository.findById(claims.userId())
                .orElseThrow(AuthenticationRequiredException::new);
        UserAccount activeUser = loginUserPolicy.requireActive(userAccount);
        if (!RolePermissionSummary.permissionsFor(activeUser.role()).contains("feeding.write")) {
            throw new FeedingAccessForbiddenException();
        }
        return activeUser;
    }

    private FeedingView toView(Feeding feeding) {
        return new FeedingView(
                feeding.id(),
                feeding.animalId(),
                feeding.lotId(),
                feeding.feedType().apiValue(),
                feeding.description(),
                feeding.quantityKg(),
                feeding.costPerKg(),
                feeding.startDate(),
                feeding.endDate(),
                feeding.source().apiValue(),
                feeding.recordedBy()
        );
    }
}
