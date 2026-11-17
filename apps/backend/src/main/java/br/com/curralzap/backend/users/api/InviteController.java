package br.com.curralzap.backend.users.api;

import br.com.curralzap.backend.shared.api.ControllerSupport;
import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.users.application.InviteService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invites")
class InviteController {

    private final InviteService inviteService;

    InviteController(InviteService inviteService) {
        this.inviteService = inviteService;
    }

    @PostMapping
    InviteApiResponse createInvite(
            HttpServletRequest request,
            @RequestBody CreateInviteApiRequest body
    ) {
        return InviteApiResponse.from(inviteService.create(requireClaims(request), body.toInput()));
    }

    @GetMapping
    List<InviteApiResponse> listInvites(HttpServletRequest request) {
        return InviteApiResponse.fromList(inviteService.list(requireClaims(request)));
    }

    @DeleteMapping("/{inviteId}")
    void revokeInvite(
            HttpServletRequest request,
            @PathVariable UUID inviteId
    ) {
        inviteService.revoke(requireClaims(request), inviteId);
    }

    private AuthTokenClaims requireClaims(HttpServletRequest request) {
        return ControllerSupport.requireClaims(request);
    }
}
