package br.com.curralzap.backend.auth.infrastructure;

import br.com.curralzap.backend.auth.application.OAuthCodeExchanger;
import br.com.curralzap.backend.auth.application.OAuthTokenVerificationException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
class FacebookOAuthCodeExchanger implements OAuthCodeExchanger {

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;

    FacebookOAuthCodeExchanger(
            RestClient.Builder restClientBuilder,
            @Value("${curralzap.oauth.facebook.client-id:}") String clientId,
            @Value("${curralzap.oauth.facebook.client-secret:}") String clientSecret
    ) {
        this.restClient = restClientBuilder.baseUrl("https://graph.facebook.com").build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public boolean supports(String provider) {
        return "facebook".equalsIgnoreCase(provider);
    }

    @Override
    public String exchangeForIdToken(String code, String codeVerifier, String redirectUri) throws OAuthTokenVerificationException {
        FacebookTokenResponse response;
        try {
            response = restClient.get()
                    .uri("/v18.0/oauth/access_token?code={code}&client_id={clientId}&client_secret={clientSecret}&redirect_uri={redirectUri}&code_verifier={codeVerifier}",
                            code, clientId, clientSecret, redirectUri, codeVerifier)
                    .retrieve()
                    .body(FacebookTokenResponse.class);
        } catch (RestClientException e) {
            throw new OAuthTokenVerificationException("Facebook code exchange failed.", e);
        }

        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            throw new OAuthTokenVerificationException("Facebook token exchange did not return an access_token.");
        }

        return response.accessToken();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record FacebookTokenResponse(
            @JsonProperty("access_token") String accessToken
    ) {
    }
}
