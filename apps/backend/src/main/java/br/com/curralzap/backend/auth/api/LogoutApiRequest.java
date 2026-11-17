package br.com.curralzap.backend.auth.api;

import jakarta.validation.constraints.NotBlank;

record LogoutApiRequest(
        @NotBlank(message = "Refresh token is required.")
        String refreshToken
) {
}
