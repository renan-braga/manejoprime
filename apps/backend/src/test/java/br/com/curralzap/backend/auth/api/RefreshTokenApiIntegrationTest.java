package br.com.curralzap.backend.auth.api;

import br.com.curralzap.backend.auth.application.AuthSessionLifecycle;
import br.com.curralzap.backend.auth.application.AuthTokenPair;
import br.com.curralzap.backend.support.BackendApiIntegrationTestSupport;
import br.com.curralzap.backend.users.domain.UserAccount;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.sql.Timestamp;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RefreshTokenApiIntegrationTest extends BackendApiIntegrationTestSupport {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthSessionLifecycle authSessionLifecycle;

    @Test
    void refreshesValidSessionAndRotatesRefreshToken() throws Exception {
        UUID farmId = insertFarm("Fazenda Refresh");
        UserAccount user = insertUser(farmId, "5511999990001", "Admin User", "admin", true);
        TokenPairResponse initialSession = completeLogin(user);

        TokenPairResponse refreshedSession = refresh(initialSession.refreshToken());

        assertThat(refreshedSession.accessToken()).isNotBlank();
        assertThat(refreshedSession.refreshToken()).isNotBlank();
        assertThat(refreshedSession.refreshToken()).isNotEqualTo(initialSession.refreshToken());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(initialSession.refreshToken())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("revoked_refresh_token"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());

        Integer refreshTokenCount = jdbcTemplate.queryForObject("select count(*) from refresh_tokens", Integer.class);
        Integer consumedTokenCount = jdbcTemplate.queryForObject(
                "select count(*) from refresh_tokens where consumed_at is not null",
                Integer.class
        );
        assertThat(refreshTokenCount).isEqualTo(2);
        assertThat(consumedTokenCount).isEqualTo(1);
    }

    @Test
    void returnsExplicitErrorForExpiredRefreshToken() throws Exception {
        UUID farmId = insertFarm("Fazenda Expirada");
        UserAccount user = insertUser(farmId, "5511999990002", "Expired User", "worker", true);
        TokenPairResponse session = completeLogin(user);

        jdbcTemplate.update(
                "update refresh_tokens set expires_at = ?, updated_at = ?",
                Timestamp.from(nowUtc().minusMinutes(1).toInstant()),
                Timestamp.from(nowUtc().minusMinutes(1).toInstant())
        );

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(session.refreshToken())))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value("expired_refresh_token"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void returnsExplicitErrorForMalformedRefreshToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "not-a-jwt"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value("invalid_refresh_token"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void logoutRevokesCurrentSessionAndBlocksFutureRefresh() throws Exception {
        UUID farmId = insertFarm("Fazenda Logout");
        UserAccount user = insertUser(farmId, "5511999990003", "Worker User", "worker", true);
        TokenPairResponse session = completeLogin(user);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(session.refreshToken())))
                .andExpect(status().isNoContent())
                .andExpect(header().exists("X-Request-Id"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(session.refreshToken())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("revoked_refresh_token"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void newLoginInvalidatesPreviousSessionChain() throws Exception {
        UUID farmId = insertFarm("Fazenda Sessao");
        UserAccount user = insertUser(farmId, "5511999990004", "Admin User", "admin", true);
        TokenPairResponse firstSession = completeLogin(user);
        TokenPairResponse secondSession = completeLogin(user);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(firstSession.refreshToken())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("session_replaced"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(secondSession.refreshToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void returnsAccessRevokedWhenAccountIsDisabledBeforeRefresh() throws Exception {
        UUID farmId = insertFarm("Fazenda Inativa");
        UserAccount user = insertUser(farmId, "5511999990005", "Worker User", "worker", true);
        TokenPairResponse session = completeLogin(user);
        jdbcTemplate.update("update users set ativo = false where telefone = ?", "5511999990005");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(session.refreshToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("access_revoked"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    private TokenPairResponse completeLogin(UserAccount user) {
        AuthTokenPair tokenPair = authSessionLifecycle.startSession(user);
        return new TokenPairResponse(tokenPair.accessToken(), tokenPair.refreshToken());
    }

    private TokenPairResponse refresh(String refreshToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return new TokenPairResponse(
                response.get("accessToken").asText(),
                response.get("refreshToken").asText()
        );
    }

    private record TokenPairResponse(String accessToken, String refreshToken) {
    }
}
