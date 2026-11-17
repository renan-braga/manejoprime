package br.com.curralzap.backend.ops.application;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OpsRecentErrorView(
        UUID messageId,
        UUID farmId,
        UUID userId,
        String failureReason,
        OffsetDateTime receivedAt
) {}
