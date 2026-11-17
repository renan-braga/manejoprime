package br.com.curralzap.backend.offers.api;

import br.com.curralzap.backend.support.BackendApiIntegrationTestSupport;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OffersApiIntegrationTest extends BackendApiIntegrationTestSupport {

    @Test
    void adminCanListOffers() throws Exception {
        UUID farmId = insertFarm("Fazenda Lista");
        UserAccount admin = insertUser(farmId, "5511900040011", "Admin Lista", UserRole.ADMIN, true);
        String token = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(post("/api/v1/offers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"Oferta A\",\"askingPrice\":10000.00,\"animalIds\":[]}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/offers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"Oferta B\",\"askingPrice\":20000.00,\"animalIds\":[]}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/offers")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].status").value("draft"))
                .andExpect(jsonPath("$.items[0].animalCount").value(0));
    }

    @Test
    void adminCanFilterOffersByStatus() throws Exception {
        UUID farmId = insertFarm("Fazenda Filtro");
        UserAccount admin = insertUser(farmId, "5511900040012", "Admin Filtro", UserRole.ADMIN, true);
        String token = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));

        String createResponse = mockMvc.perform(post("/api/v1/offers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"Ativa\",\"askingPrice\":15000.00,\"animalIds\":[]}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String offerId = com.jayway.jsonpath.JsonPath.read(createResponse, "$.id");

        mockMvc.perform(post("/api/v1/offers/{offerId}/activate", offerId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/offers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"Rascunho\",\"askingPrice\":5000.00,\"animalIds\":[]}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/offers").param("status", "active")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Ativa"));
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"VETERINARIAN", "WORKER"})
    void nonAdminCannotListOffers(UserRole role) throws Exception {
        UUID farmId = insertFarm("Fazenda Forbidden List");
        String phone = role == UserRole.VETERINARIAN ? "5511900040013" : "5511900040014";
        UserAccount user = insertUser(farmId, phone, "Forbidden List " + role.name(), role, true);
        String token = issueAccessToken(user, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/offers")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
    }

    @Test
    void adminCanCreateOfferWithAnimals() throws Exception {
        UUID farmId = insertFarm("Fazenda Criação");
        UUID lotId = insertLot(farmId, "Engorda", "fattening");
        UUID animalA = insertAnimal(farmId, lotId, "C-001");
        UUID animalB = insertAnimal(farmId, lotId, "C-002");
        UserAccount admin = insertUser(farmId, "5511900040001", "Admin Offer", UserRole.ADMIN, true);
        String token = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(post("/api/v1/offers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Lote Engorda",
                                  "notes": "Animais saudáveis.",
                                  "askingPrice": 75000.00,
                                  "buyerContact": "João — (11) 91234-5678",
                                  "expiresAt": "2026-08-31",
                                  "animalIds": ["%s", "%s"]
                                }
                                """.formatted(animalA, animalB)))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.farmId").value(farmId.toString()))
                .andExpect(jsonPath("$.title").value("Lote Engorda"))
                .andExpect(jsonPath("$.notes").value("Animais saudáveis."))
                .andExpect(jsonPath("$.askingPrice").value(75000.00))
                .andExpect(jsonPath("$.buyerContact").value("João — (11) 91234-5678"))
                .andExpect(jsonPath("$.expiresAt").value("2026-08-31"))
                .andExpect(jsonPath("$.status").value("draft"))
                .andExpect(jsonPath("$.animalIds.length()").value(2))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void adminCanUpdateOffer() throws Exception {
        UUID farmId = insertFarm("Fazenda Update");
        UUID lotId = insertLot(farmId, "Cria", "raising");
        UUID animalA = insertAnimal(farmId, lotId, "U-001");
        UUID animalB = insertAnimal(farmId, lotId, "U-002");
        UUID animalC = insertAnimal(farmId, lotId, "U-003");
        UserAccount admin = insertUser(farmId, "5511900040002", "Admin Update", UserRole.ADMIN, true);
        String token = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));

        String createResponse = mockMvc.perform(post("/api/v1/offers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"title":"Inicial","askingPrice":50000.00,"animalIds":["%s","%s"]}
                                """.formatted(animalA, animalB)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String offerId = com.jayway.jsonpath.JsonPath.read(createResponse, "$.id");

        mockMvc.perform(patch("/api/v1/offers/{offerId}", offerId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Revisada",
                                  "notes": "Preço negociável.",
                                  "askingPrice": 48000.00,
                                  "buyerContact": "Maria — (11) 91111-2222",
                                  "expiresAt": "2026-09-01",
                                  "animalIds": ["%s", "%s"]
                                }
                                """.formatted(animalA, animalC)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(offerId))
                .andExpect(jsonPath("$.title").value("Revisada"))
                .andExpect(jsonPath("$.notes").value("Preço negociável."))
                .andExpect(jsonPath("$.askingPrice").value(48000.00))
                .andExpect(jsonPath("$.animalIds.length()").value(2));
    }

    @Test
    void adminCanFollowFullLifecycle() throws Exception {
        UUID farmId = insertFarm("Fazenda Lifecycle");
        UserAccount admin = insertUser(farmId, "5511900040003", "Admin Lifecycle", UserRole.ADMIN, true);
        String token = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));

        String createResponse = mockMvc.perform(post("/api/v1/offers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"Venda\",\"askingPrice\":30000.00,\"animalIds\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("draft"))
                .andReturn().getResponse().getContentAsString();

        String offerId = com.jayway.jsonpath.JsonPath.read(createResponse, "$.id");

        mockMvc.perform(post("/api/v1/offers/{offerId}/activate", offerId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("active"));

        mockMvc.perform(post("/api/v1/offers/{offerId}/mark-sold", offerId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("sold"));
    }

    @Test
    void adminCanCancelOffer() throws Exception {
        UUID farmId = insertFarm("Fazenda Cancel");
        UserAccount admin = insertUser(farmId, "5511900040004", "Admin Cancel", UserRole.ADMIN, true);
        String token = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));

        String createResponse = mockMvc.perform(post("/api/v1/offers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"Cancelável\",\"askingPrice\":20000.00,\"animalIds\":[]}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String offerId = com.jayway.jsonpath.JsonPath.read(createResponse, "$.id");

        mockMvc.perform(post("/api/v1/offers/{offerId}/cancel", offerId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("cancelled"));
    }

    @Test
    void invalidStateTransitionReturns409() throws Exception {
        UUID farmId = insertFarm("Fazenda Invalid");
        UserAccount admin = insertUser(farmId, "5511900040005", "Admin Invalid", UserRole.ADMIN, true);
        String token = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));

        String createResponse = mockMvc.perform(post("/api/v1/offers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"Inválida\",\"askingPrice\":10000.00,\"animalIds\":[]}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String offerId = com.jayway.jsonpath.JsonPath.read(createResponse, "$.id");

        mockMvc.perform(post("/api/v1/offers/{offerId}/mark-sold", offerId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("invalid_state_transition"));
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"VETERINARIAN", "WORKER"})
    void nonAdminCannotCreateOffer(UserRole role) throws Exception {
        UUID farmId = insertFarm("Fazenda Forbidden");
        UserAccount user = insertUser(farmId, phoneFor(role), "Forbidden " + role.name(), role, true);
        String token = issueAccessToken(user, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(post("/api/v1/offers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"Não autorizado\",\"askingPrice\":5000.00,\"animalIds\":[]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
    }

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/offers")
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"Sem token\",\"askingPrice\":5000.00,\"animalIds\":[]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void offerFromOtherFarmReturns404() throws Exception {
        UUID farmA = insertFarm("Fazenda A");
        UUID farmB = insertFarm("Fazenda B");
        UserAccount adminA = insertUser(farmA, "5511900040008", "Admin A", UserRole.ADMIN, true);
        UserAccount adminB = insertUser(farmB, "5511900040009", "Admin B", UserRole.ADMIN, true);

        String createResponse = mockMvc.perform(post("/api/v1/offers")
                        .header("Authorization", "Bearer " + issueAccessToken(adminA, OffsetDateTime.now(ZoneOffset.UTC)))
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"Oferta de A\",\"askingPrice\":10000.00,\"animalIds\":[]}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String offerId = com.jayway.jsonpath.JsonPath.read(createResponse, "$.id");

        mockMvc.perform(post("/api/v1/offers/{offerId}/activate", offerId)
                        .header("Authorization", "Bearer " + issueAccessToken(adminB, OffsetDateTime.now(ZoneOffset.UTC))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not_found"));
    }

    @Test
    void validationFailureReturns400() throws Exception {
        UUID farmId = insertFarm("Fazenda Validacao");
        UserAccount admin = insertUser(farmId, "5511900040010", "Admin Validacao", UserRole.ADMIN, true);
        String token = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(post("/api/v1/offers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"\",\"askingPrice\":-1.00,\"animalIds\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors.title").exists())
                .andExpect(jsonPath("$.fieldErrors.askingPrice").exists());
    }

    private String phoneFor(UserRole role) {
        return switch (role) {
            case ADMIN -> "5511900040006";
            case VETERINARIAN -> "5511900040007";
            case WORKER -> "5511900040007";
        };
    }
}
