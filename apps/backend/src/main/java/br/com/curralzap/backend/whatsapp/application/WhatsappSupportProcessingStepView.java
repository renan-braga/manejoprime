package br.com.curralzap.backend.whatsapp.application;

import java.time.OffsetDateTime;
import java.util.UUID;

public record WhatsappSupportProcessingStepView(
        UUID id,
        String status,
        String details,
        OffsetDateTime occurredAt
) {
}
