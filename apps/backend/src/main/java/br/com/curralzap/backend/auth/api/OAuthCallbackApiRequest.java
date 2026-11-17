package br.com.curralzap.backend.auth.api;

import jakarta.validation.constraints.NotBlank;

record OAuthCallbackApiRequest(
        @NotBlank String provider,
        @NotBlank String code,
        @NotBlank String codeVerifier,
        @NotBlank String redirectUri
) {
}
