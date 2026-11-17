package br.com.curralzap.backend.auth.api;

import br.com.curralzap.backend.auth.application.OAuthCallbackResult;
import br.com.curralzap.backend.auth.application.OAuthCallbackService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/oauth")
class OAuthCallbackController {

    private final OAuthCallbackService oAuthCallbackService;

    OAuthCallbackController(OAuthCallbackService oAuthCallbackService) {
        this.oAuthCallbackService = oAuthCallbackService;
    }

    @PostMapping("/callback")
    ResponseEntity<OAuthCallbackApiResponse> callback(@Valid @RequestBody OAuthCallbackApiRequest request) {
        OAuthCallbackResult result = oAuthCallbackService.authenticate(
                request.provider(), request.code(), request.codeVerifier(), request.redirectUri()
        );
        return ResponseEntity.ok(OAuthCallbackApiResponse.from(result));
    }
}
