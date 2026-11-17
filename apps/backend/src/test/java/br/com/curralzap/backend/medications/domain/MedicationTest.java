package br.com.curralzap.backend.medications.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MedicationTest {

    @Test
    void calculatesReleaseDateFromApplicationDateAndWithdrawalPeriod() {
        UUID farmId = UUID.randomUUID();
        UUID animalId = UUID.randomUUID();
        UUID recordedBy = UUID.randomUUID();

        Medication medication = Medication.record(
                farmId,
                animalId,
                null,
                recordedBy,
                "Ivermectin",
                "Ivermectina",
                "Ticks",
                "10 ml",
                LocalDate.of(2026, 4, 20),
                35,
                new BigDecimal("45.90"),
                MedicationSource.APP
        );

        assertThat(medication.id()).isNull();
        assertThat(medication.releaseDate()).isEqualTo(LocalDate.of(2026, 5, 25));
        assertThat(medication.occurrenceId()).isNull();
    }

    @Test
    void supportsOptionalOccurrenceAssociation() {
        UUID farmId = UUID.randomUUID();
        UUID animalId = UUID.randomUUID();
        UUID occurrenceId = UUID.randomUUID();
        UUID recordedBy = UUID.randomUUID();

        Medication medication = Medication.record(
                farmId,
                animalId,
                occurrenceId,
                recordedBy,
                "Oxytetracycline",
                null,
                "Carrapato",
                "20 ml",
                LocalDate.of(2026, 4, 21),
                28,
                null,
                MedicationSource.WHATSAPP
        );

        assertThat(medication.occurrenceId()).isEqualTo(occurrenceId);
        assertThat(medication.releaseDate()).isEqualTo(LocalDate.of(2026, 5, 19));
    }

    @Test
    void rejectsNegativeWithdrawalPeriod() {
        assertThatThrownBy(() -> Medication.record(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                UUID.randomUUID(),
                "Anti-inflamatorio",
                null,
                null,
                "5 ml",
                LocalDate.of(2026, 4, 20),
                -1,
                null,
                MedicationSource.APP
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Withdrawal period");
    }
}
