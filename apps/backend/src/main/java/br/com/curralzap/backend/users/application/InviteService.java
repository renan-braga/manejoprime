package br.com.curralzap.backend.users.application;

import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.auth.application.AuthenticationRequiredException;
import br.com.curralzap.backend.auth.application.LoginUserPolicy;
import br.com.curralzap.backend.auth.application.RolePermissionSummary;
import br.com.curralzap.backend.users.domain.Invitation;
import br.com.curralzap.backend.users.domain.InvitationRepository;
import br.com.curralzap.backend.users.domain.InvitationStatus;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserAccountRepository;
import br.com.curralzap.backend.whatsapp.application.WhatsappWelcomeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class InviteService {

    private final InvitationRepository invitationRepository;
    private final UserAccountRepository userAccountRepository;
    private final LoginUserPolicy loginUserPolicy;
    private final WhatsappWelcomeService whatsappWelcomeService;

    InviteService(
            InvitationRepository invitationRepository,
            UserAccountRepository userAccountRepository,
            LoginUserPolicy loginUserPolicy,
            WhatsappWelcomeService whatsappWelcomeService
    ) {
        this.invitationRepository = invitationRepository;
        this.userAccountRepository = userAccountRepository;
        this.loginUserPolicy = loginUserPolicy;
        this.whatsappWelcomeService = whatsappWelcomeService;
    }

    @Transactional
    public InviteDetailView create(AuthTokenClaims claims, CreateInviteInput input) {
        requirePermission(claims);
        Invitation saved = invitationRepository.save(
                Invitation.create(claims.farmId(), input.email(), input.phone(), input.role(), claims.userId())
        );
        if (input.phone() != null) {
            whatsappWelcomeService.sendWelcome(input.phone(), input.email(), claims.farmId());
        }
        return toDetailView(saved);
    }

    public List<InviteDetailView> list(AuthTokenClaims claims) {
        requirePermission(claims);
        return invitationRepository.findByFarmId(claims.farmId()).stream()
                .map(this::toDetailView)
                .toList();
    }

    @Transactional
    public void revoke(AuthTokenClaims claims, UUID inviteId) {
        requirePermission(claims);
        Invitation invite = invitationRepository.findByIdAndFarmId(inviteId, claims.farmId())
                .filter(i -> i.status() == InvitationStatus.PENDING)
                .orElseThrow(InviteNotFoundException::new);
        invitationRepository.save(invite.revoke());
    }

    private InviteDetailView toDetailView(Invitation invite) {
        return new InviteDetailView(
                invite.id(),
                invite.farmId(),
                invite.email(),
                invite.phone(),
                invite.role().databaseValue(),
                invite.status().name().toLowerCase(),
                invite.invitedBy(),
                invite.createdAt()
        );
    }

    private void requirePermission(AuthTokenClaims claims) {
        UserAccount activeUser = userAccountRepository.findById(claims.userId())
                .orElseThrow(AuthenticationRequiredException::new);
        loginUserPolicy.requireActive(activeUser);
        if (!RolePermissionSummary.permissionsFor(activeUser.role()).contains("users.write")) {
            throw new InviteForbiddenException();
        }
    }
}
