package br.com.curralzap.backend.vaccines.infrastructure;

import br.com.curralzap.backend.support.BackendIntegrationTestSupport;
import br.com.curralzap.backend.users.domain.UserRole;
import br.com.curralzap.backend.vaccines.domain.Vaccine;
import br.com.curralzap.backend.vaccines.domain.VaccineDose;
import br.com.curralzap.backend.vaccines.domain.VaccineRepository;
import br.com.curralzap.backend.vaccines.domain.VaccineSource;
import br.com.curralzap.backend.shared.domain.AppendFirstMutationNotAllowedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VaccineRepositoryPersistenceTest extends BackendIntegrationTestSupport {

    @Autowired
    private VaccineRepository vaccineRepository;

    @Test
    void savesAndLoadsAnimalTargetVaccinesAndReturnsAnimalHistoryDescending() {
        UUID farmId = insertFarm("Fazenda Vacinas");
        UUID animalId = insertAnimal(farmId, null, "VAC-001");
        UUID recordedBy = insertUser(farmId, "5511999998801", "Vet Vacina", UserRole.VETERINARIAN, true).id();

        Vaccine first = vaccineRepository.save(
                Vaccine.recordForAnimal(
                        farmId,
                        animalId,
                        recordedBy,
                        "Aftosa",
                        "L-001",
                        VaccineDose.FIRST,
                        LocalDate.of(2026, 4, 1),
                        LocalDate.of(2026, 5, 1),
                        new BigDecimal("2.00"),
                        VaccineSource.APP
                )
        );

        Vaccine second = vaccineRepository.save(
                Vaccine.recordForAnimal(
                        farmId,
                        animalId,
                        recordedBy,
                        "Aftosa",
                        "L-002",
                        VaccineDose.SECOND,
                        LocalDate.of(2026, 5, 1),
                        null,
                        new BigDecimal("2.00"),
                        VaccineSource.WHATSAPP
                )
        );

        Vaccine loaded = vaccineRepository.findByIdAndFarmId(first.id(), farmId).orElseThrow();

        assertThat(loaded.animalId()).isEqualTo(animalId);
        assertThat(loaded.lotId()).isNull();
        assertThat(loaded.vaccineType()).isEqualTo("Aftosa");
        assertThat(loaded.vaccineBatch()).isEqualTo("L-001");
        assertThat(loaded.dose()).isEqualTo(VaccineDose.FIRST);
        assertThat(loaded.applicationDate()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(loaded.nextDoseDate()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(loaded.volumeMl()).isEqualByComparingTo("2.00");
        assertThat(loaded.source()).isEqualTo(VaccineSource.APP);
        assertThat(loaded.recordedBy()).isEqualTo(recordedBy);
        assertThat(loaded.createdAt()).isNotNull();
        assertThat(loaded.updatedAt()).isNotNull();

        List<Vaccine> history = vaccineRepository.findAnimalHistory(farmId, animalId);

        assertThat(history).extracting(Vaccine::id).containsExactly(second.id(), first.id());
    }

    @Test
    void savesLotTargetVaccinesAndReturnsUpcomingDosesAndLotHistory() {
        UUID farmId = insertFarm("Fazenda Calendario");
        UUID lotId = insertLot(farmId, "Recria", "raising");
        UUID recordedBy = insertUser(farmId, "5511999998802", "Admin Vacina", UserRole.ADMIN, true).id();

        Vaccine dueLater = vaccineRepository.save(
                Vaccine.recordForLot(
                        farmId,
                        lotId,
                        recordedBy,
                        "Raiva",
                        "RB-10",
                        VaccineDose.BOOSTER,
                        LocalDate.of(2026, 4, 10),
                        LocalDate.of(2026, 6, 15),
                        new BigDecimal("5.50"),
                        VaccineSource.APP
                )
        );

        Vaccine dueSooner = vaccineRepository.save(
                Vaccine.recordForLot(
                        farmId,
                        lotId,
                        recordedBy,
                        "Brucelose",
                        null,
                        VaccineDose.ANNUAL,
                        LocalDate.of(2026, 4, 12),
                        LocalDate.of(2026, 5, 20),
                        null,
                        VaccineSource.APP
                )
        );

        Vaccine noNextDose = vaccineRepository.save(
                Vaccine.recordForLot(
                        farmId,
                        lotId,
                        recordedBy,
                        "Aftosa",
                        null,
                        VaccineDose.FIRST,
                        LocalDate.of(2026, 4, 1),
                        null,
                        new BigDecimal("3.00"),
                        VaccineSource.WHATSAPP
                )
        );

        List<Vaccine> upcomingDoses = vaccineRepository.findUpcomingDoses(farmId, LocalDate.of(2026, 5, 1));

        assertThat(upcomingDoses).extracting(Vaccine::id).containsExactly(dueSooner.id(), dueLater.id());

        List<Vaccine> lotHistory = vaccineRepository.findLotHistory(farmId, lotId);

        assertThat(lotHistory).extracting(Vaccine::id).containsExactly(dueSooner.id(), dueLater.id(), noNextDose.id());
    }

    @Test
    void rejectsInPlaceUpdatesForAppendFirstVaccinationHistory() {
        UUID farmId = insertFarm("Fazenda Append Vaccine");
        UUID animalId = insertAnimal(farmId, null, "VAC-003");
        UUID recordedBy = insertUser(farmId, "5511999998803", "Vet Append Vacina", UserRole.VETERINARIAN, true).id();

        Vaccine saved = vaccineRepository.save(
                Vaccine.recordForAnimal(
                        farmId,
                        animalId,
                        recordedBy,
                        "Aftosa",
                        "L-100",
                        VaccineDose.FIRST,
                        LocalDate.of(2026, 4, 3),
                        LocalDate.of(2026, 5, 3),
                        new BigDecimal("2.00"),
                        VaccineSource.APP
                )
        );

        Vaccine attemptedUpdate = new Vaccine(
                saved.id(),
                saved.farmId(),
                saved.animalId(),
                saved.lotId(),
                saved.vaccineType(),
                "L-UPDATED",
                saved.dose(),
                saved.applicationDate(),
                saved.nextDoseDate(),
                saved.volumeMl(),
                saved.source(),
                saved.recordedBy(),
                saved.createdAt(),
                saved.updatedAt()
        );

        assertThatThrownBy(() -> vaccineRepository.save(attemptedUpdate))
                .isInstanceOf(AppendFirstMutationNotAllowedException.class)
                .hasMessageContaining("Vaccine records are append-first");

        Vaccine reloaded = vaccineRepository.findByIdAndFarmId(saved.id(), farmId).orElseThrow();
        assertThat(reloaded.vaccineBatch()).isEqualTo("L-100");
    }
}
