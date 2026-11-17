package br.com.curralzap.backend.farms.domain;

import java.util.Optional;
import java.util.UUID;

public interface FarmSettingsRepository {

    Optional<FarmSettings> findByFarmId(UUID farmId);

    FarmSettings save(FarmSettings settings);
}
