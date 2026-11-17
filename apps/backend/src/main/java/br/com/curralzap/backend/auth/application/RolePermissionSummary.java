package br.com.curralzap.backend.auth.application;

import br.com.curralzap.backend.users.domain.UserRole;

import java.util.List;

public final class RolePermissionSummary {

    private RolePermissionSummary() {
    }

    public static List<String> permissionsFor(UserRole role) {
        return switch (role) {
            case ADMIN -> List.of(
                    "dashboard.read",
                    "animals.read",
                    "animals.write",
                    "lots.read",
                    "lots.write",
                    "weighings.write",
                    "occurrences.write",
                    "vaccines.write",
                    "medications.write",
                    "feeding.write",
                    "users.read",
                    "users.write",
                    "settings.read",
                    "settings.write",
                    "whatsapp.support",
                    "offers.read",
                    "offers.write",
                    "reports.read"
            );
            case VETERINARIAN -> List.of(
                    "dashboard.read",
                    "animals.read",
                    "lots.read",
                    "weighings.write",
                    "occurrences.write",
                    "vaccines.write",
                    "medications.write",
                    "reports.health.read"
            );
            case WORKER -> List.of(
                    "dashboard.read",
                    "animals.read",
                    "lots.read",
                    "weighings.write",
                    "occurrences.write"
            );
        };
    }
}
