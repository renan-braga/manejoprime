package br.com.curralzap.backend.feeding.infrastructure;

import br.com.curralzap.backend.feeding.domain.Feeding;
import br.com.curralzap.backend.feeding.domain.FeedingRepository;
import br.com.curralzap.backend.feeding.domain.FeedingSource;
import br.com.curralzap.backend.feeding.domain.FeedingType;
import br.com.curralzap.backend.shared.domain.AppendFirstWriteGuard;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class JpaFeedingRepository implements FeedingRepository {

    private final FeedingJpaSpringRepository repository;

    JpaFeedingRepository(FeedingJpaSpringRepository repository) {
        this.repository = repository;
    }

    @Override
    public Feeding save(Feeding feeding) {
        AppendFirstWriteGuard.requireRepositoryAssignedId("Feeding", feeding.id());

        FeedingJpaEntity entity = new FeedingJpaEntity();
        entity.setFarmIdValue(feeding.farmId());
        entity.setAnimalId(feeding.animalId());
        entity.setLotId(feeding.lotId());
        entity.setFeedType(feeding.feedType().databaseValue());
        entity.setDescription(feeding.description());
        entity.setQuantityKg(feeding.quantityKg());
        entity.setCostPerKg(feeding.costPerKg());
        entity.setStartDate(feeding.startDate());
        entity.setEndDate(feeding.endDate());
        entity.setSource(feeding.source().databaseValue());
        entity.setRecordedBy(feeding.recordedBy());
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<Feeding> findByIdAndFarmId(UUID feedingId, UUID farmId) {
        return repository.findByIdAndFarmId(feedingId, farmId).map(this::toDomain);
    }

    @Override
    public List<Feeding> findAnimalHistory(UUID farmId, UUID animalId) {
        return repository.findByFarmIdAndAnimalIdOrderByStartDateDescCreatedAtDesc(farmId, animalId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Feeding> findLotHistory(UUID farmId, UUID lotId) {
        return repository.findByFarmIdAndLotIdOrderByStartDateDescCreatedAtDesc(farmId, lotId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Feeding> findActiveOnDate(UUID farmId, LocalDate referenceDate) {
        return repository.findActiveOnDate(farmId, referenceDate).stream()
                .map(this::toDomain)
                .toList();
    }

    private Feeding toDomain(FeedingJpaEntity entity) {
        return new Feeding(
                entity.getId(),
                entity.getFarmId(),
                entity.getAnimalId(),
                entity.getLotId(),
                FeedingType.fromDatabaseValue(entity.getFeedType()),
                entity.getDescription(),
                entity.getQuantityKg(),
                entity.getCostPerKg(),
                entity.getStartDate(),
                entity.getEndDate(),
                FeedingSource.fromDatabaseValue(entity.getSource()),
                entity.getRecordedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
