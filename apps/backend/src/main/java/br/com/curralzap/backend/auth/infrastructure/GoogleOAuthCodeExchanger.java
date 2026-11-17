package br.com.curralzap.backend.auth.infrastructure;

import br.com.curralzap.backend.auth.application.OAuthCodeExchanger;
import br.com.curralzap.backend.auth.application.OAuthTokenVerificationException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
class GoogleOAuthCodeExchanger implements OAuthCodeExchanger {

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;

    GoogleOAuthCodeExchanger(
            RestClient.Builder restClientBuilder,
            @Value("${curralzap.oauth.google.client-id:}") String clientId,
            @Value("${curralzap.oauth.google.client-secret:}") String clientSecret
    ) {
        this.restClient = restClientBuilder.baseUrl("https://oauth2.googleapis.com").build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public boolean supports(String provider) {
        return "google".equalsIgnoreCase(provider);
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

        GoogleTokenResponse response;
        try {
            response = restClient.post()
                    .uri("/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(GoogleTokenResponse.class);
        } catch (RestClientException e) {
            throw new OAuthTokenVerificationException("Google code exchange failed.", e);
        }

        if (response == null || response.idToken() == null || response.idToken().isBlank()) {
            throw new OAuthTokenVerificationException("Google token exchange did not return an id_token.");
        }

        return response.idToken();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GoogleTokenResponse(
            @JsonProperty("id_token") String idToken
    ) {
    }
}
