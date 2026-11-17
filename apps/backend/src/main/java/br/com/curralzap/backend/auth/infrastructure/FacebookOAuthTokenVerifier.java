package br.com.curralzap.backend.auth.infrastructure;

import br.com.curralzap.backend.auth.application.OAuthIdentity;
import br.com.curralzap.backend.auth.application.OAuthTokenVerificationException;
import br.com.curralzap.backend.auth.application.OAuthTokenVerifier;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
class FacebookOAuthTokenVerifier implements OAuthTokenVerifier {

    private final RestClient restClient;

    FacebookOAuthTokenVerifier(RestClient.Builder restClientBuilder) {
        // Facebook's Graph API returns text/javascript content type instead of application/json.
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setSupportedMediaTypes(List.of(
                MediaType.APPLICATION_JSON,
                new MediaType("text", "javascript"),
                MediaType.TEXT_PLAIN
        ));

        this.restClient = restClientBuilder
                .baseUrl("https://graph.facebook.com")
                .messageConverters(converters -> {
                    converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
                    converters.add(converter);
                })
                .build();
    }

    @Override
    public boolean supports(String provider) {
        return "facebook".equalsIgnoreCase(provider);
    }

    @Override
    public OAuthIdentity verify(String accessToken) throws OAuthTokenVerificationException {
        FacebookUserInfo userInfo;
        try {
            userInfo = restClient.get()
                    .uri("/me?fields=email,name&access_token={token}", accessToken)
                    .retrieve()
                    .body(FacebookUserInfo.class);
        } catch (HttpStatusCodeException e) {
            throw new OAuthTokenVerificationException(
                    "Facebook token verification failed: " + e.getStatusCode() + " " + e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            throw new OAuthTokenVerificationException("Facebook token verification failed.", e);
        }

        if (userInfo == null || userInfo.email() == null || userInfo.email().isBlank()) {
            throw new OAuthTokenVerificationException("Facebook token did not return a valid email.");
        }

        return new OAuthIdentity(userInfo.email(), userInfo.name());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record FacebookUserInfo(
            @JsonProperty("email") String email,
            @JsonProperty("name") String name
    ) {
    }
}
