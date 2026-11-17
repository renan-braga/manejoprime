package br.com.curralzap.backend.farms.api;

import br.com.curralzap.backend.auth.application.AuthTokenPair;
import br.com.curralzap.backend.farms.application.FarmRegistrationService;
import br.com.curralzap.backend.farms.application.OnboardingStatusService;
import br.com.curralzap.backend.shared.api.ControllerSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/onboarding")
class OnboardingController {

    private final FarmRegistrationService farmRegistrationService;
    private final OnboardingStatusService onboardingStatusService;

    OnboardingController(
            FarmRegistrationService farmRegistrationService,
            OnboardingStatusService onboardingStatusService
    ) {
        this.farmRegistrationService = farmRegistrationService;
        this.onboardingStatusService = onboardingStatusService;
    }

    @PostMapping("/register")
    ResponseEntity<FarmRegistrationApiResponse> register(@Valid @RequestBody FarmRegistrationApiRequest request) {
        AuthTokenPair tokenPair = farmRegistrationService.register(request.onboardingToken(), request.farmName());
        return ResponseEntity.ok(FarmRegistrationApiResponse.from(tokenPair));
    }

    @GetMapping("/status")
    OnboardingStatusApiResponse status(HttpServletRequest request) {
        return OnboardingStatusApiResponse.from(
                onboardingStatusService.status(ControllerSupport.requireClaims(request))
        );
    }
}
