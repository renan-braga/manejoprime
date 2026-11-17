package br.com.curralzap.backend.ops.api;

import br.com.curralzap.backend.ops.application.OpsFarmDetailView;
import br.com.curralzap.backend.ops.application.OpsRecentErrorView;
import br.com.curralzap.backend.ops.application.OpsUserActivityView;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

record OpsFarmDetailApiResponse(
        UUID farmId,
        String farmName,
        OffsetDateTime createdAt,
        List<UserActivityApiResponse> users,
        long messagesLast30d,
        long audioLast30d,
        long transcribedLast30d,
        long failedLast30d,
        List<RecentErrorApiResponse> recentErrors
) {
    static OpsFarmDetailApiResponse from(OpsFarmDetailView v) {
        return new OpsFarmDetailApiResponse(
                v.farmId(), v.farmName(), v.createdAt(),
                v.users().stream().map(UserActivityApiResponse::from).toList(),
                v.messagesLast30d(), v.audioLast30d(), v.transcribedLast30d(), v.failedLast30d(),
                v.recentErrors().stream().map(RecentErrorApiResponse::from).toList());
    }

    record UserActivityApiResponse(UUID userId, String name, String role, boolean active, OffsetDateTime lastActivity) {
        static UserActivityApiResponse from(OpsUserActivityView u) {
            return new UserActivityApiResponse(u.userId(), u.name(), u.role(), u.active(), u.lastActivity());
        }
    }

    record RecentErrorApiResponse(UUID messageId, UUID farmId, UUID userId, String failureReason, OffsetDateTime receivedAt) {
        static RecentErrorApiResponse from(OpsRecentErrorView e) {
            return new RecentErrorApiResponse(e.messageId(), e.farmId(), e.userId(), e.failureReason(), e.receivedAt());
        }
    }
}
