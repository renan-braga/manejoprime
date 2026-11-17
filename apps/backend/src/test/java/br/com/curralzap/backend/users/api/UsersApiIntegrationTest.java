package br.com.curralzap.backend.users.api;

import br.com.curralzap.backend.support.BackendApiIntegrationTestSupport;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserRole;
import org.junit.jupiter.api.Test;

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

class UsersApiIntegrationTest extends BackendApiIntegrationTestSupport {

    @Test
    void listsUsersWithFarmScopeFiltersAndStablePagination() throws Exception {
        UUID farmId = insertFarm("Fazenda Usuarios");
        UserAccount adminUser = insertUser(farmId, "5511999993000", "Admin Usuarios", UserRole.ADMIN, true);
        insertUser(farmId, "5511999993001", "Alpha Worker", UserRole.WORKER, true);
        insertUser(farmId, "5511999993002", "Beta Worker", UserRole.WORKER, false);
        insertUser(farmId, "5511999993003", "Gamma Vet", UserRole.VETERINARIAN, true);

        UUID otherFarmId = insertFarm("Outra Fazenda Usuarios");
        insertUser(otherFarmId, "5511999993004", "External Worker", UserRole.WORKER, true);

        String accessToken = issueAccessToken(adminUser, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/users")
                        .param("role", "worker")
                        .param("active", "true")
                        .param("search", "alpha")
                        .param("limit", "1")
                        .param("offset", "0")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.limit").value(1))
                .andExpect(jsonPath("$.offset").value(0))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Alpha Worker"))
                .andExpect(jsonPath("$.items[0].phone").value("+5511999993001"))
                .andExpect(jsonPath("$.items[0].role").value("worker"))
                .andExpect(jsonPath("$.items[0].active").value(true));
    }

    @Test
    void returnsValidationFailedForInvalidUserListFilters() throws Exception {
        UUID farmId = insertFarm("Fazenda Usuarios Validacao");
        UserAccount adminUser = insertUser(farmId, "5511999993100", "Admin Validador", UserRole.ADMIN, true);
        String accessToken = issueAccessToken(adminUser, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/users")
                        .param("limit", "0")
                        .param("offset", "-1")
                        .param("role", "owner")
                        .param("active", "yes")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors.limit").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.offset").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.role").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.active").isNotEmpty());
    }

    @Test
    void adminCanCreateUser() throws Exception {
        UUID farmId = insertFarm("Fazenda Criar Usuario");
        UserAccount adminUser = insertUser(farmId, "5511999993200", "Admin Criador", UserRole.ADMIN, true);
        String accessToken = issueAccessToken(adminUser, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "name": "Novo Trabalhador",
                                          "phone": "+55 (11) 99999-3201",
                                          "role": "worker"
                                        }
                                        """
                        )
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.farmId").value(farmId.toString()))
                .andExpect(jsonPath("$.name").value("Novo Trabalhador"))
                .andExpect(jsonPath("$.phone").value("+5511999993201"))
                .andExpect(jsonPath("$.role").value("worker"))
                .andExpect(jsonPath("$.active").value(true));

        Integer createdUsers = jdbcTemplate.queryForObject(
                "select count(*) from users where farm_id = ? and telefone = ? and role = ? and ativo = true",
                Integer.class,
                farmId,
                "5511999993201",
                "worker"
        );

        org.junit.jupiter.api.Assertions.assertEquals(1, createdUsers);
    }

    @Test
    void adminCanUpdateUserProfileAndRole() throws Exception {
        UUID farmId = insertFarm("Fazenda Atualizar Usuario");
        UserAccount adminUser = insertUser(farmId, "5511999993300", "Admin Atualizador", UserRole.ADMIN, true);
        UserAccount workerUser = insertUser(farmId, "5511999993301", "Worker Antigo", UserRole.WORKER, true);
        String accessToken = issueAccessToken(adminUser, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(patch("/api/v1/users/{userId}", workerUser.id())
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "name": "Veterinaria Nova",
                                          "phone": "+55 11 99999-3302",
                                          "role": "veterinarian"
                                        }
                                        """
                        )
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(workerUser.id().toString()))
                .andExpect(jsonPath("$.name").value("Veterinaria Nova"))
                .andExpect(jsonPath("$.phone").value("+5511999993302"))
                .andExpect(jsonPath("$.role").value("veterinarian"))
                .andExpect(jsonPath("$.active").value(true));

        String updatedRole = jdbcTemplate.queryForObject(
                "select role from users where id = ?",
                String.class,
                workerUser.id()
        );
        String updatedPhone = jdbcTemplate.queryForObject(
                "select telefone from users where id = ?",
                String.class,
                workerUser.id()
        );

        org.junit.jupiter.api.Assertions.assertEquals("veterinarian", updatedRole);
        org.junit.jupiter.api.Assertions.assertEquals("5511999993302", updatedPhone);
    }

    @Test
    void invalidUserPayloadReturnsFieldErrorsAndDuplicatePhoneConflict() throws Exception {
        UUID farmId = insertFarm("Fazenda Usuario Invalido");
        UserAccount adminUser = insertUser(farmId, "5511999993400", "Admin Invalidos", UserRole.ADMIN, true);
        insertUser(farmId, "5511999993401", "Telefone Existente", UserRole.WORKER, true);
        String accessToken = issueAccessToken(adminUser, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "name": "",
                                          "phone": "abc",
                                          "role": "owner"
                                        }
                                        """
                        )
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors.name").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.phone").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.role").isNotEmpty());

        mockMvc.perform(post("/api/v1/users")
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "name": "Telefone Duplicado",
                                          "phone": "+5511999993401",
                                          "role": "worker"
                                        }
                                        """
                        )
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("duplicate_phone"))
                .andExpect(jsonPath("$.fieldErrors.phone").isNotEmpty());
    }

    @Test
    void adminCanDeactivateAndReactivateUserExplicitly() throws Exception {
        UUID farmId = insertFarm("Fazenda Ativar Usuario");
        UserAccount adminUser = insertUser(farmId, "5511999993500", "Admin Ativador", UserRole.ADMIN, true);
        UserAccount workerUser = insertUser(farmId, "5511999993501", "Worker Ativo", UserRole.WORKER, true);
        String accessToken = issueAccessToken(adminUser, OffsetDateTime.now(ZoneOffset.UTC));
        String workerAccessToken = issueAccessToken(workerUser, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(post("/api/v1/users/{userId}/deactivate", workerUser.id())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(workerUser.id().toString()))
                .andExpect(jsonPath("$.active").value(false));

        Boolean inactiveInDatabase = jdbcTemplate.queryForObject(
                "select ativo from users where id = ?",
                Boolean.class,
                workerUser.id()
        );
        org.junit.jupiter.api.Assertions.assertFalse(inactiveInDatabase);

        mockMvc.perform(get("/api/v1/session/me")
                        .header("Authorization", "Bearer " + workerAccessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("access_revoked"));

        mockMvc.perform(post("/api/v1/users/{userId}/activate", workerUser.id())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(workerUser.id().toString()))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void nonAdminCannotManageUsers() throws Exception {
        UUID farmId = insertFarm("Fazenda Usuario Bloqueado");
        UserAccount workerUser = insertUser(farmId, "5511999993600", "Worker Sem Permissao", UserRole.WORKER, true);
        String accessToken = issueAccessToken(workerUser, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "name": "Bloqueado",
                                          "phone": "+5511999993601",
                                          "role": "worker"
                                        }
                                        """
                        )
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
    }

    @Test
    void returnsNotFoundForUsersOutsideTheAuthenticatedFarm() throws Exception {
        UUID farmId = insertFarm("Fazenda Usuario Um");
        UserAccount adminUser = insertUser(farmId, "5511999993700", "Admin Um", UserRole.ADMIN, true);
        UUID otherFarmId = insertFarm("Fazenda Usuario Dois");
        UserAccount foreignUser = insertUser(otherFarmId, "5511999993701", "Usuario Dois", UserRole.WORKER, true);
        String accessToken = issueAccessToken(adminUser, OffsetDateTime.now(ZoneOffset.UTC));

        mockMvc.perform(patch("/api/v1/users/{userId}", foreignUser.id())
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "name": "Nao Pode",
                                          "phone": "+5511999993702",
                                          "role": "worker"
                                        }
                                        """
                        )
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not_found"));
    }
}
