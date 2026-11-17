package br.com.curralzap.backend.offers.application;

import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.auth.application.AuthTokenType;
import br.com.curralzap.backend.offers.application.InvalidOfferStateTransitionException;
import br.com.curralzap.backend.support.BackendIntegrationTestSupport;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OfferServiceIntegrationTest extends BackendIntegrationTestSupport {

    @Autowired
    private OfferService offerService;

    @Test
    void adminCanCreateOfferWithAnimals() {
        UUID farmId = insertFarm("Fazenda Criação");
        UUID lotId = insertLot(farmId, "Engorda", "fattening");
        UUID animalA = insertAnimal(farmId, lotId, "C-001");
        UUID animalB = insertAnimal(farmId, lotId, "C-002");
        UserAccount admin = insertUser(farmId, "5511900010001", "Admin Criação", UserRole.ADMIN, true);
        AuthTokenClaims claims = claimsFor(admin);

        OfferMutationInput input = OfferMutationInput.fromApi(
                "Lote Engorda",
                "Animais saudáveis.",
                new BigDecimal("75000.00"),
                "João — (11) 91234-5678",
                LocalDate.of(2026, 8, 31),
                List.of(animalA, animalB)
        );

        OfferDetailView result = offerService.createOffer(claims, input);

        assertThat(result.id()).isNotNull();
        assertThat(result.farmId()).isEqualTo(farmId);
        assertThat(result.title()).isEqualTo("Lote Engorda");
        assertThat(result.notes()).isEqualTo("Animais saudáveis.");
        assertThat(result.askingPrice()).isEqualByComparingTo(new BigDecimal("75000.00"));
        assertThat(result.buyerContact()).isEqualTo("João — (11) 91234-5678");
        assertThat(result.expiresAt()).isEqualTo(LocalDate.of(2026, 8, 31));
        assertThat(result.status()).isEqualTo("draft");
        assertThat(result.animalIds()).containsExactlyInAnyOrder(animalA, animalB);
        assertThat(result.createdAt()).isNotNull();
    }

    @Test
    void adminCanUpdateOfferTitle() {
        UUID farmId = insertFarm("Fazenda Update");
        UUID lotId = insertLot(farmId, "Cria", "raising");
        UUID animalA = insertAnimal(farmId, lotId, "U-001");
        UUID animalB = insertAnimal(farmId, lotId, "U-002");
        UUID animalC = insertAnimal(farmId, lotId, "U-003");
        UserAccount admin = insertUser(farmId, "5511900010002", "Admin Update", UserRole.ADMIN, true);
        AuthTokenClaims claims = claimsFor(admin);

        OfferDetailView created = offerService.createOffer(claims, OfferMutationInput.fromApi(
                "Oferta inicial", null, new BigDecimal("50000.00"), null, null, List.of(animalA, animalB)
        ));

        OfferDetailView updated = offerService.updateOffer(claims, created.id(), OfferMutationInput.fromApi(
                "Oferta revisada", "Preço negociável.", new BigDecimal("48000.00"),
                "Maria — (11) 91111-2222", LocalDate.of(2026, 9, 1), List.of(animalA, animalC)
        ));

        assertThat(updated.id()).isEqualTo(created.id());
        assertThat(updated.title()).isEqualTo("Oferta revisada");
        assertThat(updated.notes()).isEqualTo("Preço negociável.");
        assertThat(updated.askingPrice()).isEqualByComparingTo(new BigDecimal("48000.00"));
        assertThat(updated.animalIds()).containsExactlyInAnyOrder(animalA, animalC);
    }

    @Test
    void adminCanFollowFullLifecycle() {
        UUID farmId = insertFarm("Fazenda Lifecycle");
        UserAccount admin = insertUser(farmId, "5511900010003", "Admin Lifecycle", UserRole.ADMIN, true);
        AuthTokenClaims claims = claimsFor(admin);

        OfferDetailView draft = offerService.createOffer(claims, OfferMutationInput.fromApi(
                "Venda", null, new BigDecimal("30000.00"), null, null, List.of()
        ));
        assertThat(draft.status()).isEqualTo("draft");

        OfferDetailView active = offerService.activateOffer(claims, draft.id());
        assertThat(active.status()).isEqualTo("active");

        OfferDetailView sold = offerService.markOfferSold(claims, draft.id());
        assertThat(sold.status()).isEqualTo("sold");
    }

    @Test
    void adminCanCancelDraftOffer() {
        UUID farmId = insertFarm("Fazenda Cancel");
        UserAccount admin = insertUser(farmId, "5511900010004", "Admin Cancel", UserRole.ADMIN, true);
        AuthTokenClaims claims = claimsFor(admin);

        OfferDetailView draft = offerService.createOffer(claims, OfferMutationInput.fromApi(
                "Cancelável", null, new BigDecimal("20000.00"), null, null, List.of()
        ));

        OfferDetailView cancelled = offerService.cancelOffer(claims, draft.id());
        assertThat(cancelled.status()).isEqualTo("cancelled");
    }

    @Test
    void invalidStateTransitionThrowsException() {
        UUID farmId = insertFarm("Fazenda Invalid");
        UserAccount admin = insertUser(farmId, "5511900010005", "Admin Invalid", UserRole.ADMIN, true);
        AuthTokenClaims claims = claimsFor(admin);

        OfferDetailView draft = offerService.createOffer(claims, OfferMutationInput.fromApi(
                "Inválida", null, new BigDecimal("10000.00"), null, null, List.of()
        ));

        assertThatThrownBy(() -> offerService.markOfferSold(claims, draft.id()))
                .isInstanceOf(InvalidOfferStateTransitionException.class);
    }

    @Test
    void nonAdminCannotCreateOffer() {
        UUID farmId = insertFarm("Fazenda Forbidden");
        UserAccount worker = insertUser(farmId, "5511900010006", "Worker Forbidden", UserRole.WORKER, true);
        AuthTokenClaims claims = claimsFor(worker);

        assertThatThrownBy(() -> offerService.createOffer(claims, OfferMutationInput.fromApi(
                "Não autorizado", null, new BigDecimal("5000.00"), null, null, List.of()
        ))).isInstanceOf(OfferAccessForbiddenException.class);
    }

    @Test
    void offerNotFoundForOtherFarm() {
        UUID farmA = insertFarm("Fazenda A");
        UUID farmB = insertFarm("Fazenda B");
        UserAccount adminA = insertUser(farmA, "5511900010007", "Admin A", UserRole.ADMIN, true);
        UserAccount adminB = insertUser(farmB, "5511900010008", "Admin B", UserRole.ADMIN, true);

        OfferDetailView offer = offerService.createOffer(claimsFor(adminA), OfferMutationInput.fromApi(
                "Oferta de A", null, new BigDecimal("10000.00"), null, null, List.of()
        ));

        assertThatThrownBy(() -> offerService.activateOffer(claimsFor(adminB), offer.id()))
                .isInstanceOf(OfferNotFoundException.class);
    }

    @Test
    void invalidInputThrowsValidationException() {
        assertThatThrownBy(() -> OfferMutationInput.fromApi(
                null, null, null, null, null, null
        )).isInstanceOf(InvalidOfferMutationException.class)
          .satisfies(ex -> {
              InvalidOfferMutationException e = (InvalidOfferMutationException) ex;
              assertThat(e.fieldErrors()).containsKey("title");
              assertThat(e.fieldErrors()).containsKey("askingPrice");
          });

        assertThatThrownBy(() -> OfferMutationInput.fromApi(
                "Título válido", null, new BigDecimal("-1.00"), null, null, null
        )).isInstanceOf(InvalidOfferMutationException.class)
          .satisfies(ex -> {
              InvalidOfferMutationException e = (InvalidOfferMutationException) ex;
              assertThat(e.fieldErrors()).containsKey("askingPrice");
          });
    }

    private AuthTokenClaims claimsFor(UserAccount user) {
        OffsetDateTime now = nowUtc();
        return new AuthTokenClaims(
                user.id(),
                user.farmId(),
                user.role(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                AuthTokenType.ACCESS,
                now,
                now.plusHours(1)
        );
    }
}
