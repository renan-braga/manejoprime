package br.com.curralzap.backend.ops.api;

import br.com.curralzap.backend.ops.application.OpsMessageErrorPageView;
import br.com.curralzap.backend.ops.application.OpsRecentErrorView;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

record OpsMessageErrorPageApiResponse(
        List<ErrorItemApiResponse> errors,
        long total,
        int limit,
        int offset
) {
    static OpsMessageErrorPageApiResponse from(OpsMessageErrorPageView v) {
        return new OpsMessageErrorPageApiResponse(
                v.errors().stream().map(ErrorItemApiResponse::from).toList(),
                v.total(), v.limit(), v.offset());
    }

    record ErrorItemApiResponse(UUID messageId, UUID farmId, UUID userId, String failureReason, OffsetDateTime receivedAt) {
        static ErrorItemApiResponse from(OpsRecentErrorView e) {
            return new ErrorItemApiResponse(e.messageId(), e.farmId(), e.userId(), e.failureReason(), e.receivedAt());
        }
    }
}
