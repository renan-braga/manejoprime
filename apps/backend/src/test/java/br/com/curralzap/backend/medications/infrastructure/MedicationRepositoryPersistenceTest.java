package br.com.curralzap.backend.medications.infrastructure;

import br.com.curralzap.backend.medications.domain.Medication;
import br.com.curralzap.backend.medications.domain.MedicationRepository;
import br.com.curralzap.backend.medications.domain.MedicationSource;
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

class MedicationRepositoryPersistenceTest extends BackendIntegrationTestSupport {

    @Autowired
    private MedicationRepository medicationRepository;

    @Test
    void savesAndLoadsMedicationWithOptionalOccurrenceAndReleaseDate() {
        UUID farmId = insertFarm("Fazenda Medicacoes");
        UUID animalId = insertAnimal(farmId, null, "MED-001");
        UUID recordedBy = insertUser(farmId, "5511999999201", "Vet Med", UserRole.VETERINARIAN, true).id();
        UUID occurrenceId = insertOccurrenceRecord(farmId, animalId, recordedBy, "open", null);

        Medication saved = medicationRepository.save(
                Medication.record(
                        farmId,
                        animalId,
                        occurrenceId,
                        recordedBy,
                        "Ivermectin",
                        "Ivermectina",
                        "Ticks",
                        "10 ml",
                        LocalDate.of(2026, 4, 20),
                        35,
                        new BigDecimal("45.90"),
                        MedicationSource.APP
                )
        );

        Medication loaded = medicationRepository.findByIdAndFarmId(saved.id(), farmId).orElseThrow();

        assertThat(loaded.animalId()).isEqualTo(animalId);
        assertThat(loaded.occurrenceId()).isEqualTo(occurrenceId);
        assertThat(loaded.medicationName()).isEqualTo("Ivermectin");
        assertThat(loaded.activeIngredient()).isEqualTo("Ivermectina");
        assertThat(loaded.reason()).isEqualTo("Ticks");
        assertThat(loaded.dosage()).isEqualTo("10 ml");
        assertThat(loaded.applicationDate()).isEqualTo(LocalDate.of(2026, 4, 20));
        assertThat(loaded.withdrawalPeriodDays()).isEqualTo(35);
        assertThat(loaded.releaseDate()).isEqualTo(LocalDate.of(2026, 5, 25));
        assertThat(loaded.cost()).isEqualByComparingTo("45.90");
        assertThat(loaded.source()).isEqualTo(MedicationSource.APP);
        assertThat(loaded.recordedBy()).isEqualTo(recordedBy);
        assertThat(loaded.createdAt()).isNotNull();
        assertThat(loaded.updatedAt()).isNotNull();
    }

    @Test
    void returnsAnimalHistoryDescendingAndActiveWithdrawalChecks() {
        UUID farmId = insertFarm("Fazenda Historico");
        UUID animalId = insertAnimal(farmId, null, "MED-002");
        UUID recordedBy = insertUser(farmId, "5511999999202", "Admin Med", UserRole.ADMIN, true).id();

        Medication activeEarly = medicationRepository.save(
                Medication.record(
                        farmId,
                        animalId,
                        null,
                        recordedBy,
                        "Ivermectin",
                        null,
                        null,
                        "10 ml",
                        LocalDate.of(2026, 4, 1),
                        35,
                        null,
                        MedicationSource.APP
                )
        );

        Medication futureMedication = medicationRepository.save(
                Medication.record(
                        farmId,
                        animalId,
                        null,
                        recordedBy,
                        "Oxytetracycline",
                        null,
                        null,
                        "20 ml",
                        LocalDate.of(2026, 6, 1),
                        28,
                        null,
                        MedicationSource.WHATSAPP
                )
        );

        Medication laterHistory = medicationRepository.save(
                Medication.record(
                        farmId,
                        animalId,
                        null,
                        recordedBy,
                        "Anti-inflamatorio",
                        null,
                        "Dor",
                        "5 ml",
                        LocalDate.of(2026, 4, 15),
                        0,
                        new BigDecimal("12.50"),
                        MedicationSource.APP
                )
        );

        List<Medication> history = medicationRepository.findAnimalHistory(farmId, animalId);

        assertThat(history).extracting(Medication::id)
                .containsExactly(futureMedication.id(), laterHistory.id(), activeEarly.id());

        List<Medication> activeOnApril20 = medicationRepository.findActiveWithdrawalByAnimalId(
                farmId,
                animalId,
                LocalDate.of(2026, 4, 20)
        );

        assertThat(activeOnApril20).extracting(Medication::id).containsExactly(activeEarly.id());

        List<Medication> activeOnJune10 = medicationRepository.findActiveWithdrawalByAnimalId(
                farmId,
                animalId,
                LocalDate.of(2026, 6, 10)
        );

        assertThat(activeOnJune10).extracting(Medication::id).containsExactly(futureMedication.id());
    }

    @Test
    void rejectsInPlaceUpdatesForAppendFirstMedicationHistory() {
        UUID farmId = insertFarm("Fazenda Append Med");
        UUID animalId = insertAnimal(farmId, null, "MED-003");
        UUID recordedBy = insertUser(farmId, "5511999999203", "Vet Append Med", UserRole.VETERINARIAN, true).id();

        Medication saved = medicationRepository.save(
                Medication.record(
                        farmId,
                        animalId,
                        null,
                        recordedBy,
                        "Ivermectin",
                        "Ivermectina",
                        "Parasites",
                        "10 ml",
                        LocalDate.of(2026, 4, 12),
                        21,
                        new BigDecimal("30.00"),
                        MedicationSource.APP
                )
        );

        Medication attemptedUpdate = new Medication(
                saved.id(),
                saved.farmId(),
                saved.animalId(),
                saved.occurrenceId(),
                saved.medicationName(),
                saved.activeIngredient(),
                "Corrected reason",
                saved.dosage(),
                saved.applicationDate(),
                saved.withdrawalPeriodDays(),
                saved.releaseDate(),
                new BigDecimal("35.00"),
                saved.source(),
                saved.recordedBy(),
                saved.createdAt(),
                saved.updatedAt()
        );

        assertThatThrownBy(() -> medicationRepository.save(attemptedUpdate))
                .isInstanceOf(AppendFirstMutationNotAllowedException.class)
                .hasMessageContaining("Medication records are append-first");

        Medication reloaded = medicationRepository.findByIdAndFarmId(saved.id(), farmId).orElseThrow();
        assertThat(reloaded.reason()).isEqualTo("Parasites");
        assertThat(reloaded.cost()).isEqualByComparingTo("30.00");
    }
}
