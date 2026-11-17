package br.com.curralzap.backend.auth.api;

import br.com.curralzap.backend.auth.application.AuthSessionLifecycle;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
class LogoutController {

    private final AuthSessionLifecycle authSessionLifecycle;

    LogoutController(AuthSessionLifecycle authSessionLifecycle) {
        this.authSessionLifecycle = authSessionLifecycle;
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(@Valid @RequestBody LogoutApiRequest request) {
        authSessionLifecycle.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
