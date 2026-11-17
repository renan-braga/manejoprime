package br.com.curralzap.backend.animals.api;

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

class AnimalImportApiIntegrationTest extends BackendApiIntegrationTestSupport {

    @Test
    void importsValidRowsAndSkipsDuplicateBrinco() throws Exception {
        UUID farmId = insertFarm("Fazenda Import Animais");
        UserAccount admin = insertUser(farmId, "5511900002001", "Admin Animais", UserRole.ADMIN, true);
        String token = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));
        UUID lotId = insertLot(farmId, "Engorda", "fattening");

        // Pre-existing animal (should be skipped)
        insertAnimal(farmId, lotId, "001");

        String body = """
                {
                  "rows": [
                    {"brinco":"001","nome":"","raca":"","sexo":"macho","dataNascimento":"","pesoNascimentoKg":"","nomeLote":"","dataEntrada":"","status":""},
                    {"brinco":"002","nome":"Vaca 2","raca":"Nelore","sexo":"femea","dataNascimento":"2020-01-15","pesoNascimentoKg":"30.5","nomeLote":"Engorda","dataEntrada":"2021-03-01","status":"ativo"}
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/animals/import")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(1))
                .andExpect(jsonPath("$.skipped").value(1))
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void rejectsRowWithInvalidSexAndLotNotFound() throws Exception {
        UUID farmId = insertFarm("Fazenda Validacao Animais");
        UserAccount admin = insertUser(farmId, "5511900002002", "Admin Val", UserRole.ADMIN, true);
        String token = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));

        String body = """
                {
                  "rows": [
                    {"brinco":"100","nome":"","raca":"","sexo":"INVALIDO","dataNascimento":"","pesoNascimentoKg":"","nomeLote":"","dataEntrada":"","status":""},
                    {"brinco":"101","nome":"","raca":"","sexo":"macho","dataNascimento":"","pesoNascimentoKg":"","nomeLote":"Inexistente","dataEntrada":"","status":""}
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/animals/import")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(0))
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[0].row").value(1))
                .andExpect(jsonPath("$.errors[1].row").value(2));
    }

    @Test
    void returns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/animals/import")
                        .contentType(APPLICATION_JSON)
                        .content("{\"rows\":[]}"))
                .andExpect(status().isUnauthorized());
    }
}
