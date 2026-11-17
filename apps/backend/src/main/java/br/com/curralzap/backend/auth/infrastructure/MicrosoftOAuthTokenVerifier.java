package br.com.curralzap.backend.auth.infrastructure;

import br.com.curralzap.backend.auth.application.OAuthIdentity;
import br.com.curralzap.backend.auth.application.OAuthTokenVerificationException;
import br.com.curralzap.backend.auth.application.OAuthTokenVerifier;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
class MicrosoftOAuthTokenVerifier implements OAuthTokenVerifier {

    private final RestClient restClient;

    MicrosoftOAuthTokenVerifier(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.baseUrl("https://graph.microsoft.com").build();
    }

    @Override
    public boolean supports(String provider) {
        return "microsoft".equalsIgnoreCase(provider);
    }

    @Override
    public OAuthIdentity verify(String accessToken) throws OAuthTokenVerificationException {
        MicrosoftUserInfo userInfo;
        try {
            userInfo = restClient.get()
                    .uri("/v1.0/me?$select=mail,displayName")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(MicrosoftUserInfo.class);
        } catch (RestClientException e) {
            throw new OAuthTokenVerificationException("Microsoft token verification failed.", e);
        }

        if (userInfo == null || userInfo.mail() == null || userInfo.mail().isBlank()) {
            throw new OAuthTokenVerificationException("Microsoft token did not return a valid email.");
        }

        return new OAuthIdentity(userInfo.mail(), userInfo.displayName());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MicrosoftUserInfo(
            @JsonProperty("mail") String mail,
            @JsonProperty("displayName") String displayName
    ) {
    }
}
