package br.com.curralzap.backend.ops.application;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OpsFarmListItemView(
        UUID farmId,
        String farmName,
        OffsetDateTime createdAt,
        long userCount,
        long animalCount,
        OffsetDateTime lastWhatsappActivity,
        long messagesLast7d,
        long errorsLast7d
) {}
