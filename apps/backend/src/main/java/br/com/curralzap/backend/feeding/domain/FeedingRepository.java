package br.com.curralzap.backend.feeding.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeedingRepository {

    Feeding save(Feeding feeding);

    Optional<Feeding> findByIdAndFarmId(UUID feedingId, UUID farmId);

    List<Feeding> findAnimalHistory(UUID farmId, UUID animalId);

    List<Feeding> findLotHistory(UUID farmId, UUID lotId);

    List<Feeding> findActiveOnDate(UUID farmId, LocalDate referenceDate);
}
