package br.com.curralzap.backend.feeding.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeedingTest {

    @Test
    void animalTargetFactoryKeepsAnimalAndEvaluatesActivePeriod() {
        UUID farmId = UUID.randomUUID();
        UUID animalId = UUID.randomUUID();
        UUID recordedBy = UUID.randomUUID();

        Feeding feeding = Feeding.recordForAnimal(
                farmId,
                animalId,
                recordedBy,
                FeedingType.FEED,
                "Racao de crescimento",
                new BigDecimal("12.50"),
                new BigDecimal("1.90"),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 10),
                FeedingSource.APP
        );

        assertThat(feeding.id()).isNull();
        assertThat(feeding.animalId()).isEqualTo(animalId);
        assertThat(feeding.lotId()).isNull();
        assertThat(feeding.isActiveOn(LocalDate.of(2026, 4, 1))).isTrue();
        assertThat(feeding.isActiveOn(LocalDate.of(2026, 4, 5))).isTrue();
        assertThat(feeding.isActiveOn(LocalDate.of(2026, 4, 11))).isFalse();
    }

    @Test
    void lotTargetFactorySupportsOpenEndedPeriods() {
        UUID farmId = UUID.randomUUID();
        UUID lotId = UUID.randomUUID();
        UUID recordedBy = UUID.randomUUID();

        Feeding feeding = Feeding.recordForLot(
                farmId,
                lotId,
                recordedBy,
                FeedingType.SILAGE,
                null,
                new BigDecimal("40.00"),
                null,
                LocalDate.of(2026, 5, 1),
                null,
                FeedingSource.WHATSAPP
        );

        assertThat(feeding.animalId()).isNull();
        assertThat(feeding.lotId()).isEqualTo(lotId);
        assertThat(feeding.isActiveOn(LocalDate.of(2026, 6, 1))).isTrue();
    }

    @Test
    void requiresExactlyOneTargetAndValidPeriod() {
        UUID farmId = UUID.randomUUID();
        UUID animalId = UUID.randomUUID();
        UUID lotId = UUID.randomUUID();
        UUID recordedBy = UUID.randomUUID();

        assertThatThrownBy(() -> new Feeding(
                UUID.randomUUID(),
                farmId,
                null,
                null,
                FeedingType.MIXED,
                null,
                new BigDecimal("10.00"),
                null,
                LocalDate.of(2026, 4, 1),
                null,
                FeedingSource.APP,
                recordedBy,
                null,
                null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Exactly one feeding target");

        assertThatThrownBy(() -> new Feeding(
                UUID.randomUUID(),
                farmId,
                animalId,
                lotId,
                FeedingType.MIXED,
                null,
                new BigDecimal("10.00"),
                null,
                LocalDate.of(2026, 4, 1),
                null,
                FeedingSource.APP,
                recordedBy,
                null,
                null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Exactly one feeding target");

        assertThatThrownBy(() -> Feeding.recordForAnimal(
                farmId,
                animalId,
                recordedBy,
                FeedingType.PASTURE,
                null,
                new BigDecimal("10.00"),
                null,
                LocalDate.of(2026, 4, 10),
                LocalDate.of(2026, 4, 1),
                FeedingSource.APP
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("End date");
    }
}
