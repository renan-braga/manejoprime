package br.com.curralzap.backend.medications.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MedicationRepository {

    Medication save(Medication medication);

    Optional<Medication> findByIdAndFarmId(UUID medicationId, UUID farmId);

    List<Medication> findAnimalHistory(UUID farmId, UUID animalId);

    List<Medication> findActiveWithdrawalByAnimalId(UUID farmId, UUID animalId, LocalDate referenceDate);

    void delete(UUID medicationId);
}
