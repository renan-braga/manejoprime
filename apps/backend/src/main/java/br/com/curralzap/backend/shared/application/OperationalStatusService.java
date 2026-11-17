package br.com.curralzap.backend.shared.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class OperationalStatusService {

    private final List<OperationalDependencyProbe> dependencyProbes;
    private final String runtimeRole;

    public OperationalStatusService(
            List<OperationalDependencyProbe> dependencyProbes,
            @Value("${curralzap.runtime.role:api}") String runtimeRole
    ) {
        this.dependencyProbes = dependencyProbes;
        this.runtimeRole = runtimeRole;
    }

    public OperationalStatusSnapshot health() {
        return new OperationalStatusSnapshot(
                "UP",
                runtimeRole,
                List.of(new DependencyStatus("process", true, "Application context is running."))
        );
    }

    public OperationalStatusSnapshot readiness() {
        List<DependencyStatus> checks = dependencyProbes.stream()
                .map(OperationalDependencyProbe::probe)
                .sorted(Comparator.comparing(DependencyStatus::name))
                .toList();

        boolean up = checks.stream().allMatch(DependencyStatus::up);
        return new OperationalStatusSnapshot(up ? "UP" : "DOWN", runtimeRole, checks);
    }
}
