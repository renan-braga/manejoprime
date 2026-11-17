package br.com.curralzap.backend.medications.infrastructure;

import br.com.curralzap.backend.medications.domain.Medication;
import br.com.curralzap.backend.medications.domain.MedicationRepository;
import br.com.curralzap.backend.medications.domain.MedicationSource;
import br.com.curralzap.backend.shared.domain.AppendFirstWriteGuard;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class JpaMedicationRepository implements MedicationRepository {

    private final MedicationJpaSpringRepository repository;

    JpaMedicationRepository(MedicationJpaSpringRepository repository) {
        this.repository = repository;
    }

    @Override
    public Medication save(Medication medication) {
        AppendFirstWriteGuard.requireRepositoryAssignedId("Medication", medication.id());

        MedicationJpaEntity entity = new MedicationJpaEntity();
        entity.setFarmIdValue(medication.farmId());
        entity.setAnimalId(medication.animalId());
        entity.setOccurrenceId(medication.occurrenceId());
        entity.setMedicationName(medication.medicationName());
        entity.setActiveIngredient(medication.activeIngredient());
        entity.setReason(medication.reason());
        entity.setDosage(medication.dosage());
        entity.setApplicationDate(medication.applicationDate());
        entity.setWithdrawalPeriodDays(medication.withdrawalPeriodDays());
        entity.setReleaseDate(medication.releaseDate());
        entity.setCost(medication.cost());
        entity.setSource(medication.source().databaseValue());
        entity.setRecordedBy(medication.recordedBy());
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<Medication> findByIdAndFarmId(UUID medicationId, UUID farmId) {
        return repository.findByIdAndFarmId(medicationId, farmId).map(this::toDomain);
    }

    @Override
    public List<Medication> findAnimalHistory(UUID farmId, UUID animalId) {
        return repository.findByFarmIdAndAnimalIdOrderByApplicationDateDescCreatedAtDesc(farmId, animalId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Medication> findActiveWithdrawalByAnimalId(UUID farmId, UUID animalId, LocalDate referenceDate) {
        return repository.findActiveWithdrawalByAnimalId(farmId, animalId, referenceDate).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void delete(UUID medicationId) {
        repository.deleteById(java.util.Objects.requireNonNull(medicationId));
    }

    private Medication toDomain(MedicationJpaEntity entity) {
        return new Medication(
                entity.getId(),
                entity.getFarmId(),
                entity.getAnimalId(),
                entity.getOccurrenceId(),
                entity.getMedicationName(),
                entity.getActiveIngredient(),
                entity.getReason(),
                entity.getDosage(),
                entity.getApplicationDate(),
                entity.getWithdrawalPeriodDays(),
                entity.getReleaseDate(),
                entity.getCost(),
                MedicationSource.fromDatabaseValue(entity.getSource()),
                entity.getRecordedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
