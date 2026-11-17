package br.com.curralzap.backend.vaccines.api;

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

class VaccineImportApiIntegrationTest extends BackendApiIntegrationTestSupport {

    @Test
    void importsVaccinesForExistingAnimals() throws Exception {
        UUID farmId = insertFarm("Fazenda Import Vacinas");
        UserAccount admin = insertUser(farmId, "5511900003001", "Admin Vacinas", UserRole.ADMIN, true);
        String token = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));
        UUID lotId = insertLot(farmId, "Engorda", "fattening");
        insertAnimal(farmId, lotId, "VAC-001");

        String body = """
                {
                  "rows": [
                    {"brincoAnimal":"VAC-001","tipoVacina":"Aftosa","loteVacina":"BATCH-2024","dose":"D1","dataAplicacao":"2024-01-15","proximaDose":"2024-07-15","volumeMl":"5"}
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/vaccines/import")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(1))
                .andExpect(jsonPath("$.skipped").value(0))
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void rejectsRowWhenAnimalBrincoNotFound() throws Exception {
        UUID farmId = insertFarm("Fazenda Vacina Erro");
        UserAccount admin = insertUser(farmId, "5511900003002", "Admin VacErr", UserRole.ADMIN, true);
        String token = issueAccessToken(admin, OffsetDateTime.now(ZoneOffset.UTC));

        String body = """
                {
                  "rows": [
                    {"brincoAnimal":"INEXISTENTE","tipoVacina":"Aftosa","loteVacina":"","dose":"D1","dataAplicacao":"2024-01-15","proximaDose":"","volumeMl":""}
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/vaccines/import")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(0))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].row").value(1));
    }

    @Test
    void returns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/vaccines/import")
                        .contentType(APPLICATION_JSON)
                        .content("{\"rows\":[]}"))
                .andExpect(status().isUnauthorized());
    }
}
