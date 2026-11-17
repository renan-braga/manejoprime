package br.com.curralzap.backend.feeding.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface FeedingJpaSpringRepository extends JpaRepository<FeedingJpaEntity, UUID> {

    Optional<FeedingJpaEntity> findByIdAndFarmId(UUID feedingId, UUID farmId);

    List<FeedingJpaEntity> findByFarmIdAndAnimalIdOrderByStartDateDescCreatedAtDesc(UUID farmId, UUID animalId);

    List<FeedingJpaEntity> findByFarmIdAndLotIdOrderByStartDateDescCreatedAtDesc(UUID farmId, UUID lotId);

    @Query("""
            select feeding
            from FeedingJpaEntity feeding
            where feeding.farmId = :farmId
              and feeding.startDate <= :referenceDate
              and (feeding.endDate is null or feeding.endDate >= :referenceDate)
            order by feeding.startDate desc, feeding.createdAt asc
            """)
    List<FeedingJpaEntity> findActiveOnDate(UUID farmId, LocalDate referenceDate);
}
