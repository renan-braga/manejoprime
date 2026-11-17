package br.com.curralzap.backend.lots.infrastructure;

import br.com.curralzap.backend.lots.application.LotUpsertInput;
import br.com.curralzap.backend.lots.domain.Lot;
import br.com.curralzap.backend.lots.domain.LotRepository;
import br.com.curralzap.backend.support.BackendIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LotRepositoryPersistenceTest extends BackendIntegrationTestSupport {

    @Autowired
    private LotRepository lotRepository;

    @Test
    void savesAndLoadsLotWithPurposeAndPastureArea() {
        UUID farmId = insertFarm("Fazenda Repositorio");

        Lot createdLot = lotRepository.save(
                Lot.create(farmId, LotUpsertInput.fromApi("Engorda 7", "fattening", "Pasto Leste"))
        );

        assertThat(createdLot.id()).isNotNull();
        assertThat(createdLot.farmId()).isEqualTo(farmId);
        assertThat(createdLot.purpose().apiValue()).isEqualTo("fattening");
        assertThat(createdLot.pastureArea()).isEqualTo("Pasto Leste");
        assertThat(createdLot.createdAt()).isNotNull();
        assertThat(createdLot.updatedAt()).isNotNull();

        Lot loadedLot = lotRepository.findByIdAndFarmId(createdLot.id(), farmId).orElseThrow();

        assertThat(loadedLot.name()).isEqualTo("Engorda 7");
        assertThat(loadedLot.purpose().apiValue()).isEqualTo("fattening");
        assertThat(loadedLot.pastureArea()).isEqualTo("Pasto Leste");

        String databasePurpose = jdbcTemplate.queryForObject(
                "select finalidade from lots where id = ?",
                String.class,
                createdLot.id()
        );

        assertThat(databasePurpose).isEqualTo("fattening");

        Lot updatedLot = lotRepository.save(
                loadedLot.update(LotUpsertInput.fromApi("Engorda 8", "raising", "Pasto Oeste"))
        );

        assertThat(updatedLot.name()).isEqualTo("Engorda 8");
        assertThat(updatedLot.purpose().apiValue()).isEqualTo("raising");
        assertThat(updatedLot.pastureArea()).isEqualTo("Pasto Oeste");
    }
}
