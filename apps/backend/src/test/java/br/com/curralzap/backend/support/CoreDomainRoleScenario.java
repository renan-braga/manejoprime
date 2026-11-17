package br.com.curralzap.backend.support;

import br.com.curralzap.backend.auth.application.RolePermissionSummary;
import br.com.curralzap.backend.users.domain.UserRole;

import java.util.List;
import java.util.stream.Stream;

public record CoreDomainRoleScenario(
        UserRole role,
        List<String> expectedPermissions,
        int createAnimalStatus,
        int createLotStatus
) {

    public static Stream<CoreDomainRoleScenario> stream() {
        return Stream.of(
                scenario(UserRole.WORKER, 403, 403),
                scenario(UserRole.VETERINARIAN, 403, 403),
                scenario(UserRole.ADMIN, 200, 200)
        );
    }

    public String apiRole() {
        return role.name().toLowerCase();
    }

    public String displayName() {
        return role.name().toLowerCase();
    }

    private static CoreDomainRoleScenario scenario(
            UserRole role,
            int createAnimalStatus,
            int createLotStatus
    ) {
        return new CoreDomainRoleScenario(
                role,
                RolePermissionSummary.permissionsFor(role),
                createAnimalStatus,
                createLotStatus
        );
    }
}
