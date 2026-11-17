package br.com.curralzap.backend.shared.domain;

import java.util.Optional;
import java.util.UUID;

public interface FarmScopedEntityRepository<T> {

    Optional<T> findByIdAndFarmId(UUID entityId, UUID farmId);
}
