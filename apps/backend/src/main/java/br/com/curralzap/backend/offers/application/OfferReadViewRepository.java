package br.com.curralzap.backend.offers.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OfferReadViewRepository {

    List<OfferListItemView> listByFarmId(UUID farmId, String statusFilter);

    Optional<OfferDetailView> findDetailByIdAndFarmId(UUID offerId, UUID farmId);
}
