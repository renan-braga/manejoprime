package br.com.curralzap.backend.auth.api;

import br.com.curralzap.backend.auth.application.AuthSessionLifecycle;
import br.com.curralzap.backend.auth.application.AuthTokenPair;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
class RefreshTokenController {

    private final AuthSessionLifecycle authSessionLifecycle;

    RefreshTokenController(AuthSessionLifecycle authSessionLifecycle) {
        this.authSessionLifecycle = authSessionLifecycle;
    }

    @PostMapping("/refresh")
    ResponseEntity<AuthTokenApiResponse> refresh(@Valid @RequestBody RefreshTokenApiRequest request) {
        AuthTokenPair result = authSessionLifecycle.refresh(request.refreshToken());
        return ResponseEntity.ok(AuthTokenApiResponse.from(result));
    }
}
