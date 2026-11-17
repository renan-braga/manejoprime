package br.com.curralzap.backend.users.api;

import br.com.curralzap.backend.shared.api.ControllerSupport;
import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.users.application.ProfileService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile")
class ProfileController {

    private final ProfileService profileService;

    ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @PatchMapping
    UserApiResponse updatePhone(
            HttpServletRequest request,
            @RequestBody ProfileUpdateApiRequest body
    ) {
        return UserApiResponse.from(profileService.updatePhone(requireClaims(request), body.phone()));
    }

    private AuthTokenClaims requireClaims(HttpServletRequest request) {
        return ControllerSupport.requireClaims(request);
    }
}
