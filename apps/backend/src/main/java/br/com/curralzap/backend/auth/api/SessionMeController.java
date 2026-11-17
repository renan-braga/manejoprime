package br.com.curralzap.backend.auth.api;

import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.auth.application.AuthenticationRequiredException;
import br.com.curralzap.backend.auth.application.SessionMeService;
import br.com.curralzap.backend.shared.api.AccessTokenAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/session")
class SessionMeController {

    private final SessionMeService sessionMeService;

    SessionMeController(SessionMeService sessionMeService) {
        this.sessionMeService = sessionMeService;
    }

    @GetMapping("/me")
    SessionMeApiResponse sessionMe(HttpServletRequest request) {
        AuthTokenClaims claims = (AuthTokenClaims) request.getAttribute(AccessTokenAuthenticationFilter.AUTH_TOKEN_CLAIMS_ATTRIBUTE);
        if (claims == null) {
            throw new AuthenticationRequiredException();
        }
        return SessionMeApiResponse.from(sessionMeService.getSession(claims));
    }
}
