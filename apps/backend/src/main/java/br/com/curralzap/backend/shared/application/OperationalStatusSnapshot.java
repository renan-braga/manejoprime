package br.com.curralzap.backend.shared.application;

import java.util.List;

public record OperationalStatusSnapshot(
        String status,
        String runtimeRole,
        List<DependencyStatus> checks
) {

    public boolean up() {
        return "UP".equals(status);
    }
}
