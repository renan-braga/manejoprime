package br.com.curralzap.backend.vaccines.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VaccineTest {

    @Test
    void animalTargetFactoryKeepsAnimalAndClearsLot() {
        UUID farmId = UUID.randomUUID();
        UUID animalId = UUID.randomUUID();
        UUID recordedBy = UUID.randomUUID();

        Vaccine vaccine = Vaccine.recordForAnimal(
                farmId,
                animalId,
                recordedBy,
                "Aftosa",
                "AB1234",
                VaccineDose.FIRST,
                LocalDate.of(2026, 4, 23),
                LocalDate.of(2026, 5, 23),
                new BigDecimal("2.50"),
                VaccineSource.APP
        );

        assertThat(vaccine.id()).isNull();
        assertThat(vaccine.farmId()).isEqualTo(farmId);
        assertThat(vaccine.animalId()).isEqualTo(animalId);
        assertThat(vaccine.lotId()).isNull();
        assertThat(vaccine.dose()).isEqualTo(VaccineDose.FIRST);
        assertThat(vaccine.source()).isEqualTo(VaccineSource.APP);
    }

    @Test
    void requiresExactlyOneTarget() {
        UUID farmId = UUID.randomUUID();
        UUID animalId = UUID.randomUUID();
        UUID lotId = UUID.randomUUID();
        UUID recordedBy = UUID.randomUUID();
        LocalDate applicationDate = LocalDate.of(2026, 4, 23);

        assertThatThrownBy(() -> new Vaccine(
                UUID.randomUUID(),
                farmId,
                null,
                null,
                "Raiva",
                null,
                VaccineDose.BOOSTER,
                applicationDate,
                null,
                null,
                VaccineSource.APP,
                recordedBy,
                null,
                null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Exactly one vaccine target");

        assertThatThrownBy(() -> new Vaccine(
                UUID.randomUUID(),
                farmId,
                animalId,
                lotId,
                "Raiva",
                null,
                VaccineDose.BOOSTER,
                applicationDate,
                null,
                null,
                VaccineSource.WHATSAPP,
                recordedBy,
                null,
                null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Exactly one vaccine target");
    }
}
