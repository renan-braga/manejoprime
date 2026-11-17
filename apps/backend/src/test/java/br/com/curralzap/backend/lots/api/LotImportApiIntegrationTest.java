package br.com.curralzap.backend.lots.api;

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

class LotImportApiIntegrationTest extends BackendApiIntegrationTestSupport {

    @Test
    void importsValidRowsAndSkipsDuplicates() throws Exception {
        UUID farmId = insertFarm("Fazenda Import Lotes");
        UserAccount admin = insertUser(farmId, "5511900001001", "Admin Import", UserRole.ADMIN, true);
        String token = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));

        // Pre-existing lot that should be skipped
        insertLot(farmId, "Engorda 1", "fattening");

        String body = """
                {
                  "rows": [
                    {"name": "Engorda 1", "purpose": "engorda", "pastureArea": "5.0"},
                    {"name": "Recria 2", "purpose": "recria", "pastureArea": ""},
                    {"name": "Cria 3", "purpose": "cria", "pastureArea": null}
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/lots/import")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(2))
                .andExpect(jsonPath("$.skipped").value(1))
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void rejectsRowWithInvalidPurpose() throws Exception {
        UUID farmId = insertFarm("Fazenda Import Erros");
        UserAccount admin = insertUser(farmId, "5511900001002", "Admin Erros", UserRole.ADMIN, true);
        String token = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));

        String body = """
                {
                  "rows": [
                    {"name": "Lote OK", "purpose": "engorda", "pastureArea": ""},
                    {"name": "Lote Ruim", "purpose": "invalido", "pastureArea": ""}
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/lots/import")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(1))
                .andExpect(jsonPath("$.skipped").value(0))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].row").value(2));
    }

    @Test
    void returns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/lots/import")
                        .contentType(APPLICATION_JSON)
                        .content("{\"rows\":[]}"))
                .andExpect(status().isUnauthorized());
    }
}
