package br.com.curralzap.backend.auth.infrastructure;

import br.com.curralzap.backend.auth.application.OAuthCodeExchanger;
import br.com.curralzap.backend.auth.application.OAuthTokenVerificationException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
class MicrosoftOAuthCodeExchanger implements OAuthCodeExchanger {

    private static final Logger log = LoggerFactory.getLogger(MicrosoftOAuthCodeExchanger.class);

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;

    MicrosoftOAuthCodeExchanger(
            RestClient.Builder restClientBuilder,
            @Value("${curralzap.oauth.microsoft.client-id:}") String clientId,
            @Value("${curralzap.oauth.microsoft.client-secret:}") String clientSecret
    ) {
        this.restClient = restClientBuilder.baseUrl("https://login.microsoftonline.com").build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public boolean supports(String provider) {
        return "microsoft".equalsIgnoreCase(provider);
    }

    @Override
    public String exchangeForIdToken(String code, String codeVerifier, String redirectUri) throws OAuthTokenVerificationException {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("code_verifier", codeVerifier);

        MicrosoftTokenResponse response;
        try {
            response = restClient.post()
                    .uri("/common/oauth2/v2.0/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), (req, res) -> {
                        String errorBody;
                        try {
                            errorBody = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        } catch (IOException ex) {
                            errorBody = "(unreadable)";
                        }
                        log.error("Microsoft token exchange failed status={} body={}", res.getStatusCode(), errorBody);
                        throw new OAuthTokenVerificationException("Microsoft code exchange failed.");
                    })
                    .body(MicrosoftTokenResponse.class);
        } catch (OAuthTokenVerificationException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("Microsoft token exchange network error: {}", e.getMessage());
            throw new OAuthTokenVerificationException("Microsoft code exchange failed.", e);
        }

        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            log.error("Microsoft token exchange returned no access_token");
            throw new OAuthTokenVerificationException("Microsoft token exchange did not return an access_token.");
        }

        return response.accessToken();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MicrosoftTokenResponse(
            @JsonProperty("access_token") String accessToken
    ) {
    }
}
