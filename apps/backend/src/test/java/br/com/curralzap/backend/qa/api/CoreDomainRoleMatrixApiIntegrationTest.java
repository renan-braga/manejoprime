package br.com.curralzap.backend.qa.api;

import br.com.curralzap.backend.support.BackendApiIntegrationTestSupport;
import br.com.curralzap.backend.support.CoreDomainRoleScenario;
import br.com.curralzap.backend.users.domain.UserAccount;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CoreDomainRoleMatrixApiIntegrationTest extends BackendApiIntegrationTestSupport {

    @ParameterizedTest(name = "session permissions stay aligned for {0}")
    @MethodSource("roleScenarios")
    void sessionPermissionsStayAlignedWithTheRoleMatrix(CoreDomainRoleScenario scenario) throws Exception {
        UUID farmId = insertFarm("Fazenda Sessao " + scenario.displayName());
        UserAccount user = insertUser(
                farmId,
                "5511999994" + phoneSuffixFor(scenario),
                "Session " + scenario.displayName(),
                scenario.role(),
                true
        );
        String accessToken = issueAccessToken(user, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/session/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.role").value(scenario.apiRole()))
                .andExpect(jsonPath("$.permissions.length()").value(scenario.expectedPermissions().size()))
                .andExpect(jsonPath("$.permissions", containsInAnyOrder(scenario.expectedPermissions().toArray())));
    }

    @ParameterizedTest(name = "shared core-domain reads stay available for {0}")
    @MethodSource("roleScenarios")
    void sharedCoreDomainReadsStayAvailableForAllSupportedRoles(CoreDomainRoleScenario scenario) throws Exception {
        UUID farmId = insertFarm("Fazenda Reads " + scenario.displayName());
        UserAccount user = insertUser(
                farmId,
                "5511999995" + phoneSuffixFor(scenario),
                "Reader " + scenario.displayName(),
                scenario.role(),
                true
        );
        UUID lotId = insertLot(farmId, "Lote " + scenario.displayName(), "raising", "Pasto A");
        UUID animalId = insertAnimal(farmId, lotId, "A-" + scenario.displayName(), "Teste", "Nelore", "macho", "ativo", null, null, null);
        String accessToken = issueAccessToken(user, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/dashboard")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"));

        mockMvc.perform(get("/api/v1/animals")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(animalId.toString()));

        mockMvc.perform(get("/api/v1/lots")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(lotId.toString()));
    }

    @ParameterizedTest(name = "mutation permissions stay aligned for {0}")
    @MethodSource("roleScenarios")
    void mutationPermissionsStayAlignedForAnimalsAndLots(CoreDomainRoleScenario scenario) throws Exception {
        UUID farmId = insertFarm("Fazenda Mutations " + scenario.displayName());
        UserAccount user = insertUser(
                farmId,
                "5511999996" + phoneSuffixFor(scenario),
                "Mutator " + scenario.displayName(),
                scenario.role(),
                true
        );
        String accessToken = issueAccessToken(user, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(post("/api/v1/animals")
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "identifier": "RM-100",
                                          "name": "Role Matrix",
                                          "breed": "Nelore",
                                          "sex": "male",
                                          "birthDate": "2025-01-05",
                                          "birthWeightKg": "30.25",
                                          "status": "active",
                                          "entryDate": "2025-03-10"
                                        }
                                        """
                        )
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().is(scenario.createAnimalStatus()))
                .andExpect(header().exists("X-Request-Id"));

        mockMvc.perform(post("/api/v1/lots")
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "name": "Lote Role Matrix",
                                          "purpose": "raising",
                                          "pastureArea": "Pasto Role"
                                        }
                                        """
                        )
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().is(scenario.createLotStatus()))
                .andExpect(header().exists("X-Request-Id"));
    }

    private static Stream<CoreDomainRoleScenario> roleScenarios() {
        return CoreDomainRoleScenario.stream();
    }

    private static String phoneSuffixFor(CoreDomainRoleScenario scenario) {
        return switch (scenario.role()) {
            case WORKER -> "001";
            case VETERINARIAN -> "002";
            case ADMIN -> "003";
        };
    }
}
