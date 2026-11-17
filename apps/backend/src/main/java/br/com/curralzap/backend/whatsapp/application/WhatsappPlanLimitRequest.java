package br.com.curralzap.backend.whatsapp.application;

import java.time.OffsetDateTime;
import java.util.UUID;

public record WhatsappPlanLimitRequest(
        UUID farmId,
        UUID userId,
        String intent,
        OffsetDateTime occurredAt
) {
}
