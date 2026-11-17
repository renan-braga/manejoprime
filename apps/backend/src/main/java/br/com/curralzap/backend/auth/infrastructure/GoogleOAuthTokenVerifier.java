package br.com.curralzap.backend.auth.infrastructure;

import br.com.curralzap.backend.auth.application.OAuthIdentity;
import br.com.curralzap.backend.auth.application.OAuthTokenVerificationException;
import br.com.curralzap.backend.auth.application.OAuthTokenVerifier;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
class GoogleOAuthTokenVerifier implements OAuthTokenVerifier {

    private final RestClient restClient;
    private final String clientId;

    GoogleOAuthTokenVerifier(
            RestClient.Builder restClientBuilder,
            @Value("${curralzap.oauth.google.client-id:}") String clientId
    ) {
        this.restClient = restClientBuilder.baseUrl("https://oauth2.googleapis.com").build();
        this.clientId = clientId;
    }

    @Override
    public boolean supports(String provider) {
        return "google".equalsIgnoreCase(provider);
    }

    @Override
    public OAuthIdentity verify(String idToken) throws OAuthTokenVerificationException {
        GoogleTokenInfo tokenInfo;
        try {
            tokenInfo = restClient.get()
                    .uri("/tokeninfo?id_token={token}", idToken)
                    .retrieve()
                    .body(GoogleTokenInfo.class);
        } catch (RestClientException e) {
            throw new OAuthTokenVerificationException("Google token verification failed.", e);
        }

        if (tokenInfo == null || tokenInfo.email() == null || tokenInfo.email().isBlank()) {
            throw new OAuthTokenVerificationException("Google token did not return a valid email.");
        }

        if (!clientId.isBlank() && !clientId.equals(tokenInfo.audience())) {
            throw new OAuthTokenVerificationException("Google token audience does not match configured client ID.");
        }

        return new OAuthIdentity(tokenInfo.email(), tokenInfo.name());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GoogleTokenInfo(
            @JsonProperty("email") String email,
            @JsonProperty("name") String name,
            @JsonProperty("aud") String audience
    ) {
    }
}
