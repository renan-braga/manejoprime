package br.com.curralzap.backend.farms.api;

import br.com.curralzap.backend.auth.application.OAuthIdentity;
import br.com.curralzap.backend.auth.application.OnboardingTokenIssuer;
import br.com.curralzap.backend.auth.application.OnboardingTokenParser;
import br.com.curralzap.backend.support.BackendApiIntegrationTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FarmOnboardingApiIntegrationTest extends BackendApiIntegrationTestSupport {

    @Autowired
    private OnboardingTokenIssuer onboardingTokenIssuer;

    @Autowired
    private OnboardingTokenParser onboardingTokenParser;

    @Autowired
    private ObjectMapper objectMapper;

    private String issueOnboardingToken(String email, String name) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return onboardingTokenIssuer.issue(new OAuthIdentity(email, name), now, now.plusMinutes(15));
    }

    private String issueExpiredOnboardingToken(String email, String name) {
        OffsetDateTime past = OffsetDateTime.now(ZoneOffset.UTC).minusHours(1);
        return onboardingTokenIssuer.issue(new OAuthIdentity(email, name), past.minusMinutes(15), past);
    }

    @Test
    void registersNewFarmAndReturnsAuthTokens() throws Exception {
        String token = issueOnboardingToken("owner@newranch.com", "João Proprietário");

        MvcResult result = mockMvc.perform(post("/api/v1/onboarding/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "onboardingToken": "%s",
                                  "farmName": "Fazenda Nova"
                                }
                                """.formatted(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();

        String accessToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();

        mockMvc.perform(get("/api/v1/session/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.farmName").value("Fazenda Nova"))
                .andExpect(jsonPath("$.role").value("admin"))
                .andExpect(jsonPath("$.name").value("João Proprietário"));
    }

    @Test
    void doubleSubmitWithSameEmailReturnsFreshSessionWithoutDuplicatingFarmOrUser() throws Exception {
        String token = issueOnboardingToken("dupecheck@ranch.com", "Maria Rancher");

        MvcResult first = mockMvc.perform(post("/api/v1/onboarding/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "onboardingToken": "%s",
                                  "farmName": "Fazenda Primeira"
                                }
                                """.formatted(token)))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken1 = objectMapper.readTree(first.getResponse().getContentAsString())
                .get("accessToken").asText();
        JsonNode session1 = objectMapper.readTree(
                mockMvc.perform(get("/api/v1/session/me")
                                .header("Authorization", "Bearer " + accessToken1))
                        .andReturn().getResponse().getContentAsString()
        );
        String farmId = session1.get("farmId").asText();

        String token2 = issueOnboardingToken("dupecheck@ranch.com", "Maria Rancher");
        MvcResult second = mockMvc.perform(post("/api/v1/onboarding/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "onboardingToken": "%s",
                                  "farmName": "Fazenda Segunda"
                                }
                                """.formatted(token2)))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken2 = objectMapper.readTree(second.getResponse().getContentAsString())
                .get("accessToken").asText();
        JsonNode session2 = objectMapper.readTree(
                mockMvc.perform(get("/api/v1/session/me")
                                .header("Authorization", "Bearer " + accessToken2))
                        .andReturn().getResponse().getContentAsString()
        );

        assertThat(session2.get("farmId").asText()).isEqualTo(farmId);
        assertThat(session2.get("farmName").asText()).isEqualTo("Fazenda Primeira");

        int farmCount = jdbcTemplate.queryForObject(
                "select count(*) from farms where nome in ('Fazenda Primeira','Fazenda Segunda')",
                Integer.class
        );
        assertThat(farmCount).isEqualTo(1);
    }

    @Test
    void expiredOnboardingTokenReturns401() throws Exception {
        String expiredToken = issueExpiredOnboardingToken("expired@ranch.com", "Expired User");

        mockMvc.perform(post("/api/v1/onboarding/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "onboardingToken": "%s",
                                  "farmName": "Fazenda Expirada"
                                }
                                """.formatted(expiredToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("token_expired"));
    }

    @Test
    void blankFarmNameReturns400WithFieldError() throws Exception {
        String token = issueOnboardingToken("blank@ranch.com", "Blank Name User");

        mockMvc.perform(post("/api/v1/onboarding/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "onboardingToken": "%s",
                                  "farmName": ""
                                }
                                """.formatted(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors.farmName").exists());
    }

    @Test
    void missingFarmNameReturns400WithFieldError() throws Exception {
        String token = issueOnboardingToken("missing@ranch.com", "Missing Name User");

        mockMvc.perform(post("/api/v1/onboarding/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "onboardingToken": "%s"
                                }
                                """.formatted(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors.farmName").exists());
    }

    @Test
    void invalidTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/onboarding/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "onboardingToken": "not.a.valid.token",
                                  "farmName": "Fazenda Qualquer"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }
}
