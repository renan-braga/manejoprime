package br.com.curralzap.backend.offers.domain;

import br.com.curralzap.backend.shared.domain.FarmScopedEntityRepository;

public interface OfferRepository extends FarmScopedEntityRepository<Offer> {

    Offer save(Offer offer);
}
