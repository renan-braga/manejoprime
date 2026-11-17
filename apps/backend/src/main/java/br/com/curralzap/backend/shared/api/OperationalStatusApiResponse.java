package br.com.curralzap.backend.shared.api;

import br.com.curralzap.backend.shared.application.OperationalStatusSnapshot;

import java.util.List;

record OperationalStatusApiResponse(
        String status,
        String runtimeRole,
        List<OperationalCheckApiResponse> checks
) {

    static OperationalStatusApiResponse from(OperationalStatusSnapshot snapshot) {
        return new OperationalStatusApiResponse(
                snapshot.status(),
                snapshot.runtimeRole(),
                snapshot.checks().stream()
                        .map(OperationalCheckApiResponse::from)
                        .toList()
        );
    }
}
