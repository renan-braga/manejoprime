package br.com.curralzap.backend.reports.application;

import java.util.List;

final class DashboardShortcutSummary {

    private DashboardShortcutSummary() {
    }

    static List<String> shortcutsFor(List<String> permissions) {
        if (permissions.contains("users.read") && permissions.contains("settings.read")) {
            return List.of("animals", "users", "settings");
        }
        if (permissions.contains("reports.health.read")) {
            return List.of("occurrence");
        }
        return List.of("weighing", "occurrence");
    }
}
