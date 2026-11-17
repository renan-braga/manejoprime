package br.com.curralzap.backend.lots.application;

import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.auth.application.AuthenticationRequiredException;
import br.com.curralzap.backend.auth.application.LoginUserPolicy;
import br.com.curralzap.backend.auth.application.RolePermissionSummary;
import br.com.curralzap.backend.shared.application.FarmScopedAccessGuard;
import br.com.curralzap.backend.lots.domain.Lot;
import br.com.curralzap.backend.lots.domain.LotRepository;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserAccountRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class LotService {

    private final UserAccountRepository userAccountRepository;
    private final LoginUserPolicy loginUserPolicy;
    private final LotReadViewRepository lotReadViewRepository;
    private final LotRepository lotRepository;
    private final FarmScopedAccessGuard farmScopedAccessGuard;

    public LotService(
            UserAccountRepository userAccountRepository,
            LoginUserPolicy loginUserPolicy,
            LotReadViewRepository lotReadViewRepository,
            LotRepository lotRepository,
            FarmScopedAccessGuard farmScopedAccessGuard
    ) {
        this.userAccountRepository = userAccountRepository;
        this.loginUserPolicy = loginUserPolicy;
        this.lotReadViewRepository = lotReadViewRepository;
        this.lotRepository = lotRepository;
        this.farmScopedAccessGuard = farmScopedAccessGuard;
    }

    public LotListView list(AuthTokenClaims claims, LotListQuery query) {
        requireReadableUser(claims);
        return lotReadViewRepository.listFor(claims.farmId(), query);
    }

    public LotDetailView detail(AuthTokenClaims claims, UUID lotId) {
        requireReadableUser(claims);
        return lotReadViewRepository.findDetailById(claims.farmId(), lotId)
                .orElseThrow(LotNotFoundException::new);
    }

    public LotDetailView create(AuthTokenClaims claims, LotUpsertInput input) {
        requireWritableUser(claims);
        Lot savedLot = lotRepository.save(Lot.create(claims.farmId(), input));
        return lotReadViewRepository.findDetailById(claims.farmId(), savedLot.id())
                .orElseThrow(LotNotFoundException::new);
    }

    public LotDetailView update(AuthTokenClaims claims, UUID lotId, LotUpsertInput input) {
        requireWritableUser(claims);
        Lot existingLot = farmScopedAccessGuard.requireInFarm(
                lotRepository,
                lotId,
                claims.farmId(),
                LotNotFoundException::new
        );
        Lot savedLot = lotRepository.save(existingLot.update(input));
        return lotReadViewRepository.findDetailById(claims.farmId(), savedLot.id())
                .orElseThrow(LotNotFoundException::new);
    }

    private UserAccount requireReadableUser(AuthTokenClaims claims) {
        UserAccount activeUser = requireActiveUser(claims);
        if (!RolePermissionSummary.permissionsFor(activeUser.role()).contains("lots.read")) {
            throw new LotReadForbiddenException();
        }
        return activeUser;
    }

    private UserAccount requireWritableUser(AuthTokenClaims claims) {
        UserAccount activeUser = requireActiveUser(claims);
        if (!RolePermissionSummary.permissionsFor(activeUser.role()).contains("lots.write")) {
            throw new LotWriteForbiddenException();
        }
        return activeUser;
    }

    private UserAccount requireActiveUser(AuthTokenClaims claims) {
        UserAccount userAccount = userAccountRepository.findById(claims.userId())
                .orElseThrow(AuthenticationRequiredException::new);
        return loginUserPolicy.requireActive(userAccount);
    }
}
