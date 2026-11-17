package br.com.curralzap.backend.offers.application;

import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.auth.application.AuthenticationRequiredException;
import br.com.curralzap.backend.auth.application.LoginUserPolicy;
import br.com.curralzap.backend.auth.application.RolePermissionSummary;
import br.com.curralzap.backend.offers.domain.Offer;
import br.com.curralzap.backend.offers.domain.OfferRepository;
import br.com.curralzap.backend.shared.application.FarmScopedAccessGuard;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserAccountRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class OfferService {

    private final UserAccountRepository userAccountRepository;
    private final LoginUserPolicy loginUserPolicy;
    private final OfferRepository offerRepository;
    private final OfferReadViewRepository offerReadViewRepository;
    private final FarmScopedAccessGuard farmScopedAccessGuard;

    public OfferService(
            UserAccountRepository userAccountRepository,
            LoginUserPolicy loginUserPolicy,
            OfferRepository offerRepository,
            OfferReadViewRepository offerReadViewRepository,
            FarmScopedAccessGuard farmScopedAccessGuard
    ) {
        this.userAccountRepository = userAccountRepository;
        this.loginUserPolicy = loginUserPolicy;
        this.offerRepository = offerRepository;
        this.offerReadViewRepository = offerReadViewRepository;
        this.farmScopedAccessGuard = farmScopedAccessGuard;
    }

    public List<OfferListItemView> listOffers(AuthTokenClaims claims, String statusFilter) {
        requireReadableUser(claims);
        return offerReadViewRepository.listByFarmId(claims.farmId(), statusFilter);
    }

    public OfferDetailView createOffer(AuthTokenClaims claims, OfferMutationInput input) {
        requireWritableUser(claims);
        Offer saved = offerRepository.save(Offer.draft(
                claims.farmId(),
                input.title(),
                input.notes(),
                input.askingPrice(),
                input.buyerContact(),
                input.expiresAt(),
                input.animalIds()
        ));
        return offerReadViewRepository.findDetailByIdAndFarmId(saved.id(), claims.farmId())
                .orElseThrow(OfferNotFoundException::new);
    }

    public OfferDetailView updateOffer(AuthTokenClaims claims, UUID offerId, OfferMutationInput input) {
        requireWritableUser(claims);
        Offer existing = findOffer(claims, offerId);
        Offer saved = offerRepository.save(existing.update(
                input.title(),
                input.notes(),
                input.askingPrice(),
                input.buyerContact(),
                input.expiresAt(),
                input.animalIds()
        ));
        return offerReadViewRepository.findDetailByIdAndFarmId(saved.id(), claims.farmId())
                .orElseThrow(OfferNotFoundException::new);
    }

    public OfferDetailView activateOffer(AuthTokenClaims claims, UUID offerId) {
        requireWritableUser(claims);
        Offer saved = offerRepository.save(findOffer(claims, offerId).activate());
        return offerReadViewRepository.findDetailByIdAndFarmId(saved.id(), claims.farmId())
                .orElseThrow(OfferNotFoundException::new);
    }

    public OfferDetailView cancelOffer(AuthTokenClaims claims, UUID offerId) {
        requireWritableUser(claims);
        Offer saved = offerRepository.save(findOffer(claims, offerId).cancel());
        return offerReadViewRepository.findDetailByIdAndFarmId(saved.id(), claims.farmId())
                .orElseThrow(OfferNotFoundException::new);
    }

    public OfferDetailView markOfferSold(AuthTokenClaims claims, UUID offerId) {
        requireWritableUser(claims);
        Offer saved = offerRepository.save(findOffer(claims, offerId).markSold());
        return offerReadViewRepository.findDetailByIdAndFarmId(saved.id(), claims.farmId())
                .orElseThrow(OfferNotFoundException::new);
    }

    private Offer findOffer(AuthTokenClaims claims, UUID offerId) {
        return farmScopedAccessGuard.requireInFarm(
                offerRepository,
                offerId,
                claims.farmId(),
                OfferNotFoundException::new
        );
    }

    private void requireReadableUser(AuthTokenClaims claims) {
        UserAccount activeUser = requireActiveUser(claims);
        if (!RolePermissionSummary.permissionsFor(activeUser.role()).contains("offers.read")) {
            throw new OfferAccessForbiddenException();
        }
    }

    private void requireWritableUser(AuthTokenClaims claims) {
        UserAccount activeUser = requireActiveUser(claims);
        if (!RolePermissionSummary.permissionsFor(activeUser.role()).contains("offers.write")) {
            throw new OfferAccessForbiddenException();
        }
    }

    private UserAccount requireActiveUser(AuthTokenClaims claims) {
        UserAccount userAccount = userAccountRepository.findById(claims.userId())
                .orElseThrow(AuthenticationRequiredException::new);
        return loginUserPolicy.requireActive(userAccount);
    }
}
