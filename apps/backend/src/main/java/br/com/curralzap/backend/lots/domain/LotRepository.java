package br.com.curralzap.backend.lots.domain;

import br.com.curralzap.backend.shared.domain.FarmScopedEntityRepository;

import java.util.Optional;
import java.util.UUID;

public interface LotRepository extends FarmScopedEntityRepository<Lot> {

    Lot save(Lot lot);

    boolean existsByFarmId(UUID farmId);

    boolean existsByFarmIdAndName(UUID farmId, String name);

    Optional<Lot> findByFarmIdAndName(UUID farmId, String name);
}
