package br.com.curralzap.backend.shared.api;

import br.com.curralzap.backend.shared.application.DependencyStatus;

record OperationalCheckApiResponse(
        String name,
        String status,
        String detail
) {

    static OperationalCheckApiResponse from(DependencyStatus status) {
        return new OperationalCheckApiResponse(status.name(), status.status(), status.detail());
    }
}
