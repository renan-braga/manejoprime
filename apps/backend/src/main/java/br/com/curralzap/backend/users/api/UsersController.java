package br.com.curralzap.backend.users.api;

import br.com.curralzap.backend.shared.api.ControllerSupport;
import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.users.application.UserListQuery;
import br.com.curralzap.backend.users.application.UserManagementService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
class UsersController {

    private final UserManagementService userManagementService;

    UsersController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping
    UserListApiResponse listUsers(
            HttpServletRequest request,
            @RequestParam(required = false) String limit,
            @RequestParam(required = false) String offset,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String active
    ) {
        return UserListApiResponse.from(
                userManagementService.list(
                        requireClaims(request),
                        UserListQuery.fromApi(limit, offset, search, role, active)
                )
        );
    }

    @PostMapping
    UserApiResponse createUser(
            HttpServletRequest request,
            @RequestBody UserMutationApiRequest body
    ) {
        return UserApiResponse.from(userManagementService.create(requireClaims(request), body.toInput()));
    }

    @PatchMapping("/{userId}")
    UserApiResponse updateUser(
            HttpServletRequest request,
            @PathVariable UUID userId,
            @RequestBody UserMutationApiRequest body
    ) {
        return UserApiResponse.from(userManagementService.update(requireClaims(request), userId, body.toInput()));
    }

    @PostMapping("/{userId}/activate")
    UserApiResponse activateUser(
            HttpServletRequest request,
            @PathVariable UUID userId
    ) {
        return UserApiResponse.from(userManagementService.activate(requireClaims(request), userId));
    }

    @PostMapping("/{userId}/deactivate")
    UserApiResponse deactivateUser(
            HttpServletRequest request,
            @PathVariable UUID userId
    ) {
        return UserApiResponse.from(userManagementService.deactivate(requireClaims(request), userId));
    }

    private AuthTokenClaims requireClaims(HttpServletRequest request) {
        return ControllerSupport.requireClaims(request);
    }
}
