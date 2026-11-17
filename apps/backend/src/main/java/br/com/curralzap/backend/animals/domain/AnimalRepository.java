package br.com.curralzap.backend.animals.domain;

import br.com.curralzap.backend.shared.domain.FarmScopedEntityRepository;

import java.util.Optional;
import java.util.UUID;

public interface AnimalRepository extends FarmScopedEntityRepository<Animal> {

    Animal save(Animal animal);

    void delete(UUID animalId);

    Optional<Animal> findByFarmIdAndIdentifier(UUID farmId, String identifier);

    boolean existsByFarmIdAndIdentifier(UUID farmId, String identifier);

    boolean existsByFarmIdAndIdentifierAndIdNot(UUID farmId, String identifier, UUID animalId);

    long countByFarmId(UUID farmId);

    boolean existsByFarmId(UUID farmId);
}
