package br.com.curralzap.backend.auth.api;

import jakarta.validation.constraints.NotBlank;

record RefreshTokenApiRequest(
        @NotBlank(message = "Refresh token is required.")
        String refreshToken
) {
}
