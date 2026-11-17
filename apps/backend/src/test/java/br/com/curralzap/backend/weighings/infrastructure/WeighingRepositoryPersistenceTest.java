package br.com.curralzap.backend.weighings.infrastructure;

import br.com.curralzap.backend.shared.domain.AppendFirstMutationNotAllowedException;
import br.com.curralzap.backend.support.BackendIntegrationTestSupport;
import br.com.curralzap.backend.users.domain.UserRole;
import br.com.curralzap.backend.weighings.application.WeighingCreateCommand;
import br.com.curralzap.backend.weighings.domain.Weighing;
import br.com.curralzap.backend.weighings.domain.WeighingRepository;
import br.com.curralzap.backend.weighings.domain.WeighingType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WeighingRepositoryPersistenceTest extends BackendIntegrationTestSupport {

    @Autowired
    private WeighingRepository weighingRepository;

    @Test
    void savesAndLoadsWeighingsForIdempotencyAndLatestLookup() {
        UUID farmId = insertFarm("Fazenda Pesagens");
        UUID animalId = insertAnimal(farmId, null, "P-001");
        UUID recordedBy = insertUser(farmId, "5511999999301", "Admin Pesagem", UserRole.ADMIN, true).id();

        Weighing first = weighingRepository.save(Weighing.create(
                farmId,
                recordedBy,
                new WeighingCreateCommand(
                        animalId,
                        new BigDecimal("250.00"),
                        LocalDate.of(2026, 4, 1),
                        WeighingType.ROUTINE
                ),
                "weighing-1"
        ));

        Weighing second = weighingRepository.save(Weighing.create(
                farmId,
                recordedBy,
                new WeighingCreateCommand(
                        animalId,
                        new BigDecimal("275.00"),
                        LocalDate.of(2026, 4, 15),
                        WeighingType.ROUTINE
                ),
                "weighing-2"
        ));

        Weighing loaded = weighingRepository.findByFarmIdAndIdempotencyKey(farmId, "weighing-1").orElseThrow();

        assertThat(loaded.id()).isEqualTo(first.id());
        assertThat(loaded.weightKg()).isEqualByComparingTo("250.00");
        assertThat(loaded.recordedBy()).isEqualTo(recordedBy);

        Weighing latestBefore = weighingRepository.findLatestBefore(farmId, animalId, LocalDate.of(2026, 4, 20)).orElseThrow();
        assertThat(latestBefore.id()).isEqualTo(second.id());
    }

    @Test
    void rejectsInPlaceUpdatesForAppendFirstWeighingHistory() {
        UUID farmId = insertFarm("Fazenda Append Weighing");
        UUID animalId = insertAnimal(farmId, null, "P-002");
        UUID recordedBy = insertUser(farmId, "5511999999302", "Admin Append Pesagem", UserRole.ADMIN, true).id();

        Weighing saved = weighingRepository.save(Weighing.create(
                farmId,
                recordedBy,
                new WeighingCreateCommand(
                        animalId,
                        new BigDecimal("260.00"),
                        LocalDate.of(2026, 4, 10),
                        WeighingType.ROUTINE
                ),
                "weighing-3"
        ));

        Weighing attemptedUpdate = new Weighing(
                saved.id(),
                saved.farmId(),
                saved.animalId(),
                new BigDecimal("299.00"),
                saved.weighingDate(),
                saved.type(),
                saved.source(),
                saved.recordedBy(),
                saved.idempotencyKey(),
                saved.createdAt(),
                saved.updatedAt()
        );

        assertThatThrownBy(() -> weighingRepository.save(attemptedUpdate))
                .isInstanceOf(AppendFirstMutationNotAllowedException.class)
                .hasMessageContaining("Weighing records are append-first");

        Weighing reloaded = weighingRepository.findByFarmIdAndIdempotencyKey(farmId, "weighing-3").orElseThrow();
        assertThat(reloaded.weightKg()).isEqualByComparingTo("260.00");
    }
}
