package br.com.curralzap.backend.offers.infrastructure;

import br.com.curralzap.backend.offers.domain.Offer;
import br.com.curralzap.backend.offers.domain.OfferRepository;
import br.com.curralzap.backend.offers.domain.OfferStatus;
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

class OfferRepositoryPersistenceTest extends BackendIntegrationTestSupport {

    @Autowired
    private OfferRepository offerRepository;

    @Test
    void savesAndLoadsDraftOfferWithAnimals() {
        UUID farmId = insertFarm("Fazenda Offer");
        UUID lotId = insertLot(farmId, "Engorda", "fattening");
        UUID animalA = insertAnimal(farmId, lotId, "O-001");
        UUID animalB = insertAnimal(farmId, lotId, "O-002");

        Offer draft = Offer.draft(
                farmId,
                "Lote de engorda para venda",
                "Animais saudáveis, bem nutridos.",
                new BigDecimal("85000.00"),
                "João Silva — (11) 99999-0001",
                LocalDate.of(2026, 6, 30),
                List.of(animalA, animalB)
        );

        Offer saved = offerRepository.save(draft);

        assertThat(saved.id()).isNotNull();
        assertThat(saved.farmId()).isEqualTo(farmId);
        assertThat(saved.title()).isEqualTo("Lote de engorda para venda");
        assertThat(saved.notes()).isEqualTo("Animais saudáveis, bem nutridos.");
        assertThat(saved.askingPrice()).isEqualByComparingTo(new BigDecimal("85000.00"));
        assertThat(saved.buyerContact()).isEqualTo("João Silva — (11) 99999-0001");
        assertThat(saved.expiresAt()).isEqualTo(LocalDate.of(2026, 6, 30));
        assertThat(saved.status()).isEqualTo(OfferStatus.DRAFT);
        assertThat(saved.animalIds()).containsExactlyInAnyOrder(animalA, animalB);
        assertThat(saved.createdAt()).isNotNull();
        assertThat(saved.updatedAt()).isNotNull();

        Offer loaded = offerRepository.findByIdAndFarmId(saved.id(), farmId).orElseThrow();

        assertThat(loaded.title()).isEqualTo("Lote de engorda para venda");
        assertThat(loaded.status()).isEqualTo(OfferStatus.DRAFT);
        assertThat(loaded.animalIds()).containsExactlyInAnyOrder(animalA, animalB);
    }

    @Test
    void updatesOfferAndReplacesAnimalLinks() {
        UUID farmId = insertFarm("Fazenda Update");
        UUID lotId = insertLot(farmId, "Cria", "raising");
        UUID animalA = insertAnimal(farmId, lotId, "U-001");
        UUID animalB = insertAnimal(farmId, lotId, "U-002");
        UUID animalC = insertAnimal(farmId, lotId, "U-003");

        Offer saved = offerRepository.save(Offer.draft(
                farmId, "Oferta inicial", null,
                new BigDecimal("50000.00"), null, null,
                List.of(animalA, animalB)
        ));

        Offer updated = offerRepository.save(saved.update(
                "Oferta revisada", "Preço negociável.",
                new BigDecimal("48000.00"), "Maria — (11) 91111-2222",
                LocalDate.of(2026, 7, 15),
                List.of(animalA, animalC)
        ));

        assertThat(updated.id()).isEqualTo(saved.id());
        assertThat(updated.title()).isEqualTo("Oferta revisada");
        assertThat(updated.askingPrice()).isEqualByComparingTo(new BigDecimal("48000.00"));
        assertThat(updated.animalIds()).containsExactlyInAnyOrder(animalA, animalC);
    }

    @Test
    void followsStatusLifecycleDraftToActiveThenSold() {
        UUID farmId = insertFarm("Fazenda Lifecycle");
        insertUser(farmId, "5511999990700", "Lifecycle Admin", UserRole.ADMIN, true);

        Offer saved = offerRepository.save(Offer.draft(
                farmId, "Venda de gado", null,
                new BigDecimal("30000.00"), null, null,
                List.of()
        ));
        assertThat(saved.status()).isEqualTo(OfferStatus.DRAFT);

        Offer activated = offerRepository.save(saved.activate());
        assertThat(activated.status()).isEqualTo(OfferStatus.ACTIVE);

        Offer sold = offerRepository.save(activated.markSold());
        assertThat(sold.status()).isEqualTo(OfferStatus.SOLD);

        String dbStatus = jdbcTemplate.queryForObject(
                "select status from offers where id = ?", String.class, sold.id()
        );
        assertThat(dbStatus).isEqualTo("sold");
    }

    @Test
    void rejectsInvalidStateTransitions() {
        UUID farmId = insertFarm("Fazenda Invalid");

        Offer saved = offerRepository.save(Offer.draft(
                farmId, "Oferta inválida", null,
                new BigDecimal("20000.00"), null, null,
                List.of()
        ));

        assertThatThrownBy(saved::markSold)
                .isInstanceOf(br.com.curralzap.backend.offers.application.InvalidOfferStateTransitionException.class);

        Offer cancelled = offerRepository.save(saved.cancel());
        assertThat(cancelled.status()).isEqualTo(OfferStatus.CANCELLED);

        assertThatThrownBy(cancelled::activate)
                .isInstanceOf(br.com.curralzap.backend.offers.application.InvalidOfferStateTransitionException.class);
    }

    @Test
    void doesNotLoadOfferFromOtherFarm() {
        UUID farmA = insertFarm("Fazenda A");
        UUID farmB = insertFarm("Fazenda B");

        Offer saved = offerRepository.save(Offer.draft(
                farmA, "Oferta da A", null,
                new BigDecimal("10000.00"), null, null,
                List.of()
        ));

        assertThat(offerRepository.findByIdAndFarmId(saved.id(), farmB)).isEmpty();
        assertThat(offerRepository.findByIdAndFarmId(saved.id(), farmA)).isPresent();
    }
}
