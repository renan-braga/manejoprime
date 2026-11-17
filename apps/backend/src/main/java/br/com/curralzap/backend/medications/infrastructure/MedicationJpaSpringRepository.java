package br.com.curralzap.backend.medications.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface MedicationJpaSpringRepository extends JpaRepository<MedicationJpaEntity, UUID> {

    Optional<MedicationJpaEntity> findByIdAndFarmId(UUID medicationId, UUID farmId);

    List<MedicationJpaEntity> findByFarmIdAndAnimalIdOrderByApplicationDateDescCreatedAtDesc(UUID farmId, UUID animalId);

    @Query("""
            select medication
            from MedicationJpaEntity medication
            where medication.farmId = :farmId
              and medication.animalId = :animalId
              and medication.applicationDate <= :referenceDate
              and medication.releaseDate >= :referenceDate
            order by medication.releaseDate asc, medication.createdAt asc
            """)
    List<MedicationJpaEntity> findActiveWithdrawalByAnimalId(UUID farmId, UUID animalId, LocalDate referenceDate);
}
