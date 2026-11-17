package br.com.curralzap.backend.offers.infrastructure;

import br.com.curralzap.backend.offers.domain.Offer;
import br.com.curralzap.backend.offers.domain.OfferRepository;
import br.com.curralzap.backend.offers.domain.OfferStatus;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
class JpaOfferRepository implements OfferRepository {

    private final OfferJpaSpringRepository repository;

    JpaOfferRepository(OfferJpaSpringRepository repository) {
        this.repository = repository;
    }

    @Override
    public Offer save(Offer offer) {
        OfferJpaEntity entity = offer.id() == null
                ? new OfferJpaEntity()
                : repository.findById(offer.id()).orElseGet(OfferJpaEntity::new);
        entity.setFarmIdValue(offer.farmId());
        entity.setTitle(offer.title());
        entity.setNotes(offer.notes());
        entity.setAskingPrice(offer.askingPrice());
        entity.setBuyerContact(offer.buyerContact());
        entity.setExpiresAt(offer.expiresAt());
        entity.setStatus(offer.status().databaseValue());
        entity.setAnimalIds(offer.animalIds());
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<Offer> findByIdAndFarmId(UUID offerId, UUID farmId) {
        return repository.findByIdAndFarmId(offerId, farmId).map(this::toDomain);
    }

    private Offer toDomain(OfferJpaEntity entity) {
        return new Offer(
                entity.getId(),
                entity.getFarmId(),
                entity.getTitle(),
                entity.getNotes(),
                entity.getAskingPrice(),
                entity.getBuyerContact(),
                entity.getExpiresAt(),
                OfferStatus.fromDatabaseValue(entity.getStatus()),
                entity.getAnimalIds(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
