package br.com.curralzap.backend.ops.application;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OpsFarmDetailView(
        UUID farmId,
        String farmName,
        OffsetDateTime createdAt,
        List<OpsUserActivityView> users,
        long messagesLast30d,
        long audioLast30d,
        long transcribedLast30d,
        long failedLast30d,
        List<OpsRecentErrorView> recentErrors
) {}
