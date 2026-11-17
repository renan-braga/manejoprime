package br.com.curralzap.backend.ops.application;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OpsUserActivityView(
        UUID userId,
        String name,
        String role,
        boolean active,
        OffsetDateTime lastActivity
) {}
