package br.com.curralzap.backend.shared.api;

import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.auth.application.AuthenticationRequiredException;
import jakarta.servlet.http.HttpServletRequest;

public final class ControllerSupport {

    private ControllerSupport() {}

    public static AuthTokenClaims requireClaims(HttpServletRequest request) {
        AuthTokenClaims claims = (AuthTokenClaims) request.getAttribute(
                AccessTokenAuthenticationFilter.AUTH_TOKEN_CLAIMS_ATTRIBUTE);
        if (claims == null) {
            throw new AuthenticationRequiredException();
        }
        return claims;
    }
}
