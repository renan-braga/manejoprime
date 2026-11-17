package br.com.curralzap.backend.weighings.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WeighingRepository {

    Weighing save(Weighing weighing);

    Optional<Weighing> findByFarmIdAndIdempotencyKey(UUID farmId, String idempotencyKey);

    Optional<Weighing> findLatestBefore(UUID farmId, UUID animalId, LocalDate weighingDate);

    List<Weighing> findHistory(UUID farmId, UUID animalId);
}
