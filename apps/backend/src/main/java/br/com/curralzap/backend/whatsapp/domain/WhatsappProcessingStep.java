package br.com.curralzap.backend.whatsapp.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public record WhatsappProcessingStep(
        UUID id,
        UUID inboundMessageId,
        WhatsappProcessingStatus status,
        String details,
        OffsetDateTime occurredAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
