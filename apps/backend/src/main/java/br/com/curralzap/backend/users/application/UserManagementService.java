package br.com.curralzap.backend.users.application;

import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.auth.application.AuthenticationRequiredException;
import br.com.curralzap.backend.auth.application.LoginUserPolicy;
import br.com.curralzap.backend.auth.application.RolePermissionSummary;
import br.com.curralzap.backend.shared.application.FarmScopedAccessGuard;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserManagementService {

    private final UserAccountRepository userAccountRepository;
    private final LoginUserPolicy loginUserPolicy;
    private final UserReadViewRepository userReadViewRepository;
    private final FarmScopedAccessGuard farmScopedAccessGuard;

    UserManagementService(
            UserAccountRepository userAccountRepository,
            LoginUserPolicy loginUserPolicy,
            UserReadViewRepository userReadViewRepository,
            FarmScopedAccessGuard farmScopedAccessGuard
    ) {
        this.userAccountRepository = userAccountRepository;
        this.loginUserPolicy = loginUserPolicy;
        this.userReadViewRepository = userReadViewRepository;
        this.farmScopedAccessGuard = farmScopedAccessGuard;
    }

    public UserListView list(AuthTokenClaims claims, UserListQuery query) {
        requirePermission(claims, "users.read");
        return userReadViewRepository.listFor(claims.farmId(), query);
    }

    @Transactional
    public UserDetailView create(AuthTokenClaims claims, UserMutationInput input) {
        requirePermission(claims, "users.write");
        requirePhoneAvailable(input.phone(), null);

        UserAccount savedUser = userAccountRepository.save(
                UserAccount.create(claims.farmId(), input.phone(), null, input.name(), input.role())
        );
        return toDetailView(savedUser);
    }

    @Transactional
    public UserDetailView update(AuthTokenClaims claims, UUID userId, UserMutationInput input) {
        requirePermission(claims, "users.write");
        UserAccount existingUser = farmScopedAccessGuard.requireInFarm(
                userAccountRepository,
                userId,
                claims.farmId(),
                UserNotFoundException::new
        );
        requirePhoneAvailable(input.phone(), existingUser.id());

        UserAccount savedUser = userAccountRepository.save(existingUser.update(input.phone(), input.name(), input.role()));
        return toDetailView(savedUser);
    }

    @Transactional
    public UserDetailView activate(AuthTokenClaims claims, UUID userId) {
        return changeActivation(claims, userId, true);
    }

    @Transactional
    public UserDetailView deactivate(AuthTokenClaims claims, UUID userId) {
        return changeActivation(claims, userId, false);
    }

    private UserDetailView changeActivation(AuthTokenClaims claims, UUID userId, boolean active) {
        requirePermission(claims, "users.write");
        UserAccount existingUser = farmScopedAccessGuard.requireInFarm(
                userAccountRepository,
                userId,
                claims.farmId(),
                UserNotFoundException::new
        );
        UserAccount savedUser = userAccountRepository.save(active ? existingUser.activate() : existingUser.deactivate());
        return toDetailView(savedUser);
    }

    private UserDetailView toDetailView(UserAccount userAccount) {
        return new UserDetailView(
                userAccount.id(),
                userAccount.farmId(),
                userAccount.name(),
                userAccount.phone(),
                userAccount.role().databaseValue(),
                userAccount.active()
        );
    }

    private void requirePhoneAvailable(String phone, UUID currentUserId) {
        userAccountRepository.findByPhone(phone)
                .filter(userAccount -> !userAccount.id().equals(currentUserId))
                .ifPresent(userAccount -> {
                    throw new DuplicateUserPhoneException();
                });
    }

    private void requirePermission(AuthTokenClaims claims, String permission) {
        UserAccount activeUser = userAccountRepository.findById(claims.userId())
                .orElseThrow(AuthenticationRequiredException::new);
        activeUser = loginUserPolicy.requireActive(activeUser);
        if (!RolePermissionSummary.permissionsFor(activeUser.role()).contains(permission)) {
            throw new UserManagementForbiddenException();
        }
    }
}
