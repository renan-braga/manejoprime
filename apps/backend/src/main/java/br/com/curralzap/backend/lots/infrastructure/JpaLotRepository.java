package br.com.curralzap.backend.lots.infrastructure;

import br.com.curralzap.backend.lots.domain.Lot;
import br.com.curralzap.backend.lots.domain.LotPurpose;
import br.com.curralzap.backend.lots.domain.LotRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
class JpaLotRepository implements LotRepository {

    private final LotJpaSpringRepository repository;

    JpaLotRepository(LotJpaSpringRepository repository) {
        this.repository = repository;
    }

    @Override
    public Lot save(Lot lot) {
        LotJpaEntity entity = lot.id() == null
                ? new LotJpaEntity()
                : repository.findById(lot.id()).orElseGet(LotJpaEntity::new);
        entity.setFarmIdValue(lot.farmId());
        entity.setName(lot.name());
        entity.setPurpose(lot.purpose().databaseValue());
        entity.setPastureArea(lot.pastureArea());
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<Lot> findByIdAndFarmId(UUID lotId, UUID farmId) {
        return repository.findByIdAndFarmId(lotId, farmId).map(this::toDomain);
    }

    @Override
    public boolean existsByFarmId(UUID farmId) {
        return repository.existsByFarmId(farmId);
    }

    @Override
    public boolean existsByFarmIdAndName(UUID farmId, String name) {
        return repository.existsByFarmIdAndName(farmId, name);
    }

    @Override
    public Optional<Lot> findByFarmIdAndName(UUID farmId, String name) {
        return repository.findByFarmIdAndName(farmId, name).map(this::toDomain);
    }

    private Lot toDomain(LotJpaEntity entity) {
        return new Lot(
                entity.getId(),
                entity.getFarmId(),
                entity.getName(),
                LotPurpose.fromDatabaseValue(entity.getPurpose()),
                entity.getPastureArea(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
