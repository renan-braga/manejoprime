package br.com.curralzap.backend.farms.api;

import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.auth.application.AuthTokenPair;
import br.com.curralzap.backend.farms.application.FarmSwitchService;
import br.com.curralzap.backend.shared.api.ControllerSupport;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth/switch-farm")
class FarmSwitchController {

    private final FarmSwitchService farmSwitchService;

    FarmSwitchController(FarmSwitchService farmSwitchService) {
        this.farmSwitchService = farmSwitchService;
    }

    @PostMapping("/{farmId}")
    FarmSwitchApiResponse switchFarm(
            HttpServletRequest request,
            @PathVariable UUID farmId
    ) {
        AuthTokenClaims claims = ControllerSupport.requireClaims(request);
        AuthTokenPair tokenPair = farmSwitchService.switchFarm(claims, farmId);
        return FarmSwitchApiResponse.from(tokenPair);
    }
}
