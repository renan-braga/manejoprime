package br.com.curralzap.backend.farms.api;

import jakarta.validation.constraints.NotBlank;

record FarmRegistrationApiRequest(
        @NotBlank String onboardingToken,
        @NotBlank String farmName
) {
}
