package br.com.curralzap.backend.users.api;

import br.com.curralzap.backend.shared.api.ControllerSupport;
import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.users.application.UserManagementService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/members")
class MembersController {

    private final UserManagementService userManagementService;

    MembersController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @DeleteMapping("/{userId}")
    UserApiResponse revokeMember(
            HttpServletRequest request,
            @PathVariable UUID userId
    ) {
        return UserApiResponse.from(userManagementService.deactivate(requireClaims(request), userId));
    }

    private AuthTokenClaims requireClaims(HttpServletRequest request) {
        return ControllerSupport.requireClaims(request);
    }
}
