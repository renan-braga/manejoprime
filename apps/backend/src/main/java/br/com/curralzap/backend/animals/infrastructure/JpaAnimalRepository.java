package br.com.curralzap.backend.animals.infrastructure;

import br.com.curralzap.backend.animals.domain.Animal;
import br.com.curralzap.backend.animals.domain.AnimalRepository;
import br.com.curralzap.backend.animals.domain.AnimalSex;
import br.com.curralzap.backend.animals.domain.AnimalStatus;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
class JpaAnimalRepository implements AnimalRepository {

    private final AnimalJpaSpringRepository repository;

    JpaAnimalRepository(AnimalJpaSpringRepository repository) {
        this.repository = repository;
    }

    @Override
    public Animal save(Animal animal) {
        AnimalJpaEntity entity = animal.id() == null
                ? new AnimalJpaEntity()
                : repository.findById(animal.id()).orElseGet(AnimalJpaEntity::new);
        entity.setFarmIdValue(animal.farmId());
        entity.setLotId(animal.lotId());
        entity.setIdentifier(animal.identifier());
        entity.setName(animal.name());
        entity.setBreed(animal.breed());
        entity.setSex(animal.sex().databaseValue());
        entity.setBirthDate(animal.birthDate());
        entity.setBirthWeightKg(animal.birthWeightKg());
        entity.setDamId(animal.damId());
        entity.setSireId(animal.sireId());
        entity.setStatus(animal.status().databaseValue());
        entity.setEntryDate(animal.entryDate());
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<Animal> findByIdAndFarmId(UUID animalId, UUID farmId) {
        return repository.findByIdAndFarmId(animalId, farmId).map(this::toDomain);
    }

    @Override
    public void delete(UUID animalId) {
        repository.deleteById(java.util.Objects.requireNonNull(animalId));
    }

    @Override
    public Optional<Animal> findByFarmIdAndIdentifier(UUID farmId, String identifier) {
        return repository.findByFarmIdAndIdentifier(farmId, identifier).map(this::toDomain);
    }

    @Override
    public boolean existsByFarmIdAndIdentifier(UUID farmId, String identifier) {
        return repository.existsByFarmIdAndIdentifier(farmId, identifier);
    }

    @Override
    public boolean existsByFarmIdAndIdentifierAndIdNot(UUID farmId, String identifier, UUID animalId) {
        return repository.existsByFarmIdAndIdentifierAndIdNot(farmId, identifier, animalId);
    }

    @Override
    public long countByFarmId(UUID farmId) {
        return repository.countByFarmId(farmId);
    }

    @Override
    public boolean existsByFarmId(UUID farmId) {
        return repository.existsByFarmId(farmId);
    }

    private Animal toDomain(AnimalJpaEntity entity) {
        return new Animal(
                entity.getId(),
                entity.getFarmId(),
                entity.getLotId(),
                entity.getIdentifier(),
                entity.getName(),
                entity.getBreed(),
                AnimalSex.fromDatabaseValue(entity.getSex()),
                entity.getBirthDate(),
                entity.getBirthWeightKg(),
                entity.getDamId(),
                entity.getSireId(),
                AnimalStatus.fromDatabaseValue(entity.getStatus()),
                entity.getEntryDate(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
