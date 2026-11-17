package br.com.curralzap.backend.farms.api;

import br.com.curralzap.backend.shared.api.ControllerSupport;
import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.farms.application.FarmSettingsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings")
class SettingsController {

    private final FarmSettingsService farmSettingsService;

    SettingsController(FarmSettingsService farmSettingsService) {
        this.farmSettingsService = farmSettingsService;
    }

    @GetMapping
    SettingsApiResponse getSettings(HttpServletRequest request) {
        return SettingsApiResponse.from(farmSettingsService.get(requireClaims(request)));
    }

    @PatchMapping
    SettingsApiResponse updateSettings(
            HttpServletRequest request,
            @RequestBody SettingsUpdateApiRequest body
    ) {
        return SettingsApiResponse.from(farmSettingsService.update(requireClaims(request), body.toInput()));
    }

    private AuthTokenClaims requireClaims(HttpServletRequest request) {
        return ControllerSupport.requireClaims(request);
    }
}
