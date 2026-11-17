package br.com.curralzap.backend.feeding.api;

import br.com.curralzap.backend.support.BackendApiIntegrationTestSupport;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserRole;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FeedingImportApiIntegrationTest extends BackendApiIntegrationTestSupport {

    @Test
    void importsForExistingAnimal() throws Exception {
        UUID farmId = insertFarm("Fazenda Import Alimentacao");
        UserAccount admin = insertUser(farmId, "5511900004001", "Admin Feed Import", UserRole.ADMIN, true);
        String token = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));
        insertAnimal(farmId, null, "FEED-001");

        String body = """
                {
                  "rows": [
                    {
                      "brincoAnimal": "FEED-001",
                      "nomeLote": "",
                      "tipoAlimento": "racao",
                      "descricao": "Ração starter",
                      "quantidadeKg": "5.5",
                      "custoPorKg": "2.30",
                      "dataInicio": "2024-01-15",
                      "dataFim": "2024-02-15"
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/feedings/import")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(1))
                .andExpect(jsonPath("$.skipped").value(0))
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void rejectsRowWhenBothAnimalAndLotProvided() throws Exception {
        UUID farmId = insertFarm("Fazenda Import Feed XOR");
        UserAccount admin = insertUser(farmId, "5511900004002", "Admin Feed XOR", UserRole.ADMIN, true);
        String token = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));
        insertAnimal(farmId, null, "FEED-XOR-001");
        insertLot(farmId, "Lote XOR", "fattening");

        String body = """
                {
                  "rows": [
                    {
                      "brincoAnimal": "FEED-XOR-001",
                      "nomeLote": "Lote XOR",
                      "tipoAlimento": "silagem",
                      "descricao": "",
                      "quantidadeKg": "10",
                      "custoPorKg": "",
                      "dataInicio": "2024-01-01",
                      "dataFim": ""
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/feedings/import")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(0))
                .andExpect(jsonPath("$.errors[0].row").value(1));
    }

    @Test
    void returns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/feedings/import")
                        .contentType(APPLICATION_JSON)
                        .content("{\"rows\":[]}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("authentication_required"));
    }
}
