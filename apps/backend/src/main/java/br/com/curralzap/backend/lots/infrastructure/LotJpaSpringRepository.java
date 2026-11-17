package br.com.curralzap.backend.lots.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface LotJpaSpringRepository extends JpaRepository<LotJpaEntity, UUID> {

    Optional<LotJpaEntity> findByIdAndFarmId(UUID lotId, UUID farmId);

    boolean existsByFarmId(UUID farmId);

    boolean existsByFarmIdAndName(UUID farmId, String name);

    Optional<LotJpaEntity> findByFarmIdAndName(UUID farmId, String name);
}
