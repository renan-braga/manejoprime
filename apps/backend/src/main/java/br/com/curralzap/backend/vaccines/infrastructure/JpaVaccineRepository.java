package br.com.curralzap.backend.vaccines.infrastructure;

import br.com.curralzap.backend.vaccines.domain.Vaccine;
import br.com.curralzap.backend.vaccines.domain.VaccineDose;
import br.com.curralzap.backend.vaccines.domain.VaccineRepository;
import br.com.curralzap.backend.vaccines.domain.VaccineSource;
import br.com.curralzap.backend.shared.domain.AppendFirstWriteGuard;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class JpaVaccineRepository implements VaccineRepository {

    private final VaccineJpaSpringRepository repository;

    JpaVaccineRepository(VaccineJpaSpringRepository repository) {
        this.repository = repository;
    }

    @Override
    public Vaccine save(Vaccine vaccine) {
        AppendFirstWriteGuard.requireRepositoryAssignedId("Vaccine", vaccine.id());

        VaccineJpaEntity entity = new VaccineJpaEntity();
        entity.setFarmIdValue(vaccine.farmId());
        entity.setAnimalId(vaccine.animalId());
        entity.setLotId(vaccine.lotId());
        entity.setVaccineType(vaccine.vaccineType());
        entity.setVaccineBatch(vaccine.vaccineBatch());
        entity.setDose(vaccine.dose().databaseValue());
        entity.setApplicationDate(vaccine.applicationDate());
        entity.setNextDoseDate(vaccine.nextDoseDate());
        entity.setVolumeMl(vaccine.volumeMl());
        entity.setSource(vaccine.source().databaseValue());
        entity.setRecordedBy(vaccine.recordedBy());
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<Vaccine> findByIdAndFarmId(UUID vaccineId, UUID farmId) {
        return repository.findByIdAndFarmId(vaccineId, farmId).map(this::toDomain);
    }

    @Override
    public List<Vaccine> findAnimalHistory(UUID farmId, UUID animalId) {
        return repository.findByFarmIdAndAnimalIdOrderByApplicationDateDescCreatedAtDesc(farmId, animalId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Vaccine> findLotHistory(UUID farmId, UUID lotId) {
        return repository.findByFarmIdAndLotIdOrderByApplicationDateDescCreatedAtDesc(farmId, lotId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void delete(UUID vaccineId) {
        repository.deleteById(java.util.Objects.requireNonNull(vaccineId));
    }

    @Override
    public boolean existsByFarmId(UUID farmId) {
        return repository.existsByFarmId(farmId);
    }

    @Override
    public List<Vaccine> findUpcomingDoses(UUID farmId, LocalDate dueOnOrAfter) {
        return repository.findUpcomingDoses(farmId, dueOnOrAfter).stream()
                .map(this::toDomain)
                .toList();
    }

    private Vaccine toDomain(VaccineJpaEntity entity) {
        return new Vaccine(
                entity.getId(),
                entity.getFarmId(),
                entity.getAnimalId(),
                entity.getLotId(),
                entity.getVaccineType(),
                entity.getVaccineBatch(),
                VaccineDose.fromDatabaseValue(entity.getDose()),
                entity.getApplicationDate(),
                entity.getNextDoseDate(),
                entity.getVolumeMl(),
                VaccineSource.fromDatabaseValue(entity.getSource()),
                entity.getRecordedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
