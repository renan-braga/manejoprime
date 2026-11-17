package br.com.curralzap.backend.vaccines.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VaccineRepository {

    Vaccine save(Vaccine vaccine);

    Optional<Vaccine> findByIdAndFarmId(UUID vaccineId, UUID farmId);

    List<Vaccine> findAnimalHistory(UUID farmId, UUID animalId);

    List<Vaccine> findLotHistory(UUID farmId, UUID lotId);

    List<Vaccine> findUpcomingDoses(UUID farmId, LocalDate dueOnOrAfter);

    void delete(UUID vaccineId);

    boolean existsByFarmId(UUID farmId);
}
