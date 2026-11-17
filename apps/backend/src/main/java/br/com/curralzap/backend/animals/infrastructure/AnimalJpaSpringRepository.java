package br.com.curralzap.backend.animals.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface AnimalJpaSpringRepository extends JpaRepository<AnimalJpaEntity, UUID> {

    Optional<AnimalJpaEntity> findByIdAndFarmId(UUID animalId, UUID farmId);

    Optional<AnimalJpaEntity> findByFarmIdAndIdentifier(UUID farmId, String identifier);

    boolean existsByFarmIdAndIdentifier(UUID farmId, String identifier);

    boolean existsByFarmIdAndIdentifierAndIdNot(UUID farmId, String identifier, UUID animalId);

    long countByFarmId(UUID farmId);

    boolean existsByFarmId(UUID farmId);
}
