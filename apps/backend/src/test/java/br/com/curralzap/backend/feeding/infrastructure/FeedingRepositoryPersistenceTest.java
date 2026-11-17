package br.com.curralzap.backend.feeding.infrastructure;

import br.com.curralzap.backend.feeding.domain.Feeding;
import br.com.curralzap.backend.feeding.domain.FeedingRepository;
import br.com.curralzap.backend.feeding.domain.FeedingSource;
import br.com.curralzap.backend.feeding.domain.FeedingType;
import br.com.curralzap.backend.shared.domain.AppendFirstMutationNotAllowedException;
import br.com.curralzap.backend.support.BackendIntegrationTestSupport;
import br.com.curralzap.backend.users.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeedingRepositoryPersistenceTest extends BackendIntegrationTestSupport {

    @Autowired
    private FeedingRepository feedingRepository;

    @Test
    void savesAndLoadsAnimalTargetFeedingAndReturnsAnimalHistoryDescending() {
        UUID farmId = insertFarm("Fazenda Alimentacao Animal");
        UUID animalId = insertAnimal(farmId, null, "ALIM-001");
        UUID recordedBy = insertUser(farmId, "5511999999401", "Admin Feed", UserRole.ADMIN, true).id();

        Feeding first = feedingRepository.save(
                Feeding.recordForAnimal(
                        farmId,
                        animalId,
                        recordedBy,
                        FeedingType.FEED,
                        "Racao inicial",
                        new BigDecimal("8.50"),
                        new BigDecimal("2.10"),
                        LocalDate.of(2026, 4, 1),
                        LocalDate.of(2026, 4, 10),
                        FeedingSource.APP
                )
        );

        Feeding second = feedingRepository.save(
                Feeding.recordForAnimal(
                        farmId,
                        animalId,
                        recordedBy,
                        FeedingType.SUPPLEMENT,
                        "Suplemento proteico",
                        new BigDecimal("2.00"),
                        null,
                        LocalDate.of(2026, 4, 15),
                        null,
                        FeedingSource.WHATSAPP
                )
        );

        Feeding loaded = feedingRepository.findByIdAndFarmId(first.id(), farmId).orElseThrow();

        assertThat(loaded.animalId()).isEqualTo(animalId);
        assertThat(loaded.lotId()).isNull();
        assertThat(loaded.feedType()).isEqualTo(FeedingType.FEED);
        assertThat(loaded.description()).isEqualTo("Racao inicial");
        assertThat(loaded.quantityKg()).isEqualByComparingTo("8.50");
        assertThat(loaded.costPerKg()).isEqualByComparingTo("2.10");
        assertThat(loaded.startDate()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(loaded.endDate()).isEqualTo(LocalDate.of(2026, 4, 10));
        assertThat(loaded.source()).isEqualTo(FeedingSource.APP);
        assertThat(loaded.recordedBy()).isEqualTo(recordedBy);
        assertThat(loaded.createdAt()).isNotNull();
        assertThat(loaded.updatedAt()).isNotNull();

        List<Feeding> history = feedingRepository.findAnimalHistory(farmId, animalId);

        assertThat(history).extracting(Feeding::id).containsExactly(second.id(), first.id());
    }

    @Test
    void savesLotTargetFeedingAndReturnsLotHistoryAndActiveFeedings() {
        UUID farmId = insertFarm("Fazenda Alimentacao Lote");
        UUID lotId = insertLot(farmId, "Engorda", "fattening");
        UUID animalId = insertAnimal(farmId, lotId, "ALIM-002");
        UUID recordedBy = insertUser(farmId, "5511999999402", "Manager Feed", UserRole.ADMIN, true).id();

        Feeding activeLot = feedingRepository.save(
                Feeding.recordForLot(
                        farmId,
                        lotId,
                        recordedBy,
                        FeedingType.SILAGE,
                        "Silagem de milho",
                        new BigDecimal("25.00"),
                        new BigDecimal("0.95"),
                        LocalDate.of(2026, 5, 1),
                        null,
                        FeedingSource.APP
                )
        );

        Feeding endedAnimal = feedingRepository.save(
                Feeding.recordForAnimal(
                        farmId,
                        animalId,
                        recordedBy,
                        FeedingType.PASTURE,
                        null,
                        new BigDecimal("5.00"),
                        null,
                        LocalDate.of(2026, 4, 1),
                        LocalDate.of(2026, 4, 20),
                        FeedingSource.WHATSAPP
                )
        );

        List<Feeding> lotHistory = feedingRepository.findLotHistory(farmId, lotId);
        assertThat(lotHistory).extracting(Feeding::id).containsExactly(activeLot.id());

        List<Feeding> activeOnMay10 = feedingRepository.findActiveOnDate(farmId, LocalDate.of(2026, 5, 10));
        assertThat(activeOnMay10).extracting(Feeding::id).containsExactly(activeLot.id());

        List<Feeding> activeOnApril10 = feedingRepository.findActiveOnDate(farmId, LocalDate.of(2026, 4, 10));
        assertThat(activeOnApril10).extracting(Feeding::id).containsExactly(endedAnimal.id());
    }

    @Test
    void rejectsInPlaceUpdatesForAppendFirstFeedingHistory() {
        UUID farmId = insertFarm("Fazenda Append Feed");
        UUID animalId = insertAnimal(farmId, null, "ALIM-003");
        UUID recordedBy = insertUser(farmId, "5511999999403", "Admin Append Feed", UserRole.ADMIN, true).id();

        Feeding saved = feedingRepository.save(
                Feeding.recordForAnimal(
                        farmId,
                        animalId,
                        recordedBy,
                        FeedingType.FEED,
                        "Racao base",
                        new BigDecimal("7.00"),
                        new BigDecimal("1.90"),
                        LocalDate.of(2026, 4, 5),
                        null,
                        FeedingSource.APP
                )
        );

        Feeding attemptedUpdate = new Feeding(
                saved.id(),
                saved.farmId(),
                saved.animalId(),
                saved.lotId(),
                saved.feedType(),
                "Racao corrigida",
                new BigDecimal("9.00"),
                saved.costPerKg(),
                saved.startDate(),
                saved.endDate(),
                saved.source(),
                saved.recordedBy(),
                saved.createdAt(),
                saved.updatedAt()
        );

        assertThatThrownBy(() -> feedingRepository.save(attemptedUpdate))
                .isInstanceOf(AppendFirstMutationNotAllowedException.class)
                .hasMessageContaining("Feeding records are append-first");

        Feeding reloaded = feedingRepository.findByIdAndFarmId(saved.id(), farmId).orElseThrow();
        assertThat(reloaded.description()).isEqualTo("Racao base");
        assertThat(reloaded.quantityKg()).isEqualByComparingTo("7.00");
    }
}
