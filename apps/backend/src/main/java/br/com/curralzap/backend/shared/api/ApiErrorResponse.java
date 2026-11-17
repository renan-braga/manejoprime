package br.com.curralzap.backend.shared.api;

import java.util.Map;

record ApiErrorResponse(
        String code,
        String message,
        Map<String, String> fieldErrors,
        Map<String, Object> details,
        String requestId
) {
}
