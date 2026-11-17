package br.com.curralzap.backend.vaccines.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface VaccineJpaSpringRepository extends JpaRepository<VaccineJpaEntity, UUID> {

    Optional<VaccineJpaEntity> findByIdAndFarmId(UUID vaccineId, UUID farmId);

    List<VaccineJpaEntity> findByFarmIdAndAnimalIdOrderByApplicationDateDescCreatedAtDesc(UUID farmId, UUID animalId);

    List<VaccineJpaEntity> findByFarmIdAndLotIdOrderByApplicationDateDescCreatedAtDesc(UUID farmId, UUID lotId);

    boolean existsByFarmId(UUID farmId);

    @Query("""
            select vaccine
            from VaccineJpaEntity vaccine
            where vaccine.farmId = :farmId
              and vaccine.nextDoseDate is not null
              and vaccine.nextDoseDate >= :dueOnOrAfter
            order by vaccine.nextDoseDate asc, vaccine.createdAt asc
            """)
    List<VaccineJpaEntity> findUpcomingDoses(UUID farmId, LocalDate dueOnOrAfter);
}
