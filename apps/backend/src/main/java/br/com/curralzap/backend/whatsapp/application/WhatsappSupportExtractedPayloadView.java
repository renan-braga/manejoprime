package br.com.curralzap.backend.whatsapp.application;

import java.time.OffsetDateTime;
import java.util.UUID;

public record WhatsappSupportExtractedPayloadView(
        UUID id,
        String provider,
        String providerModel,
        String intent,
        String structuredPayload,
        OffsetDateTime extractedAt
) {
}
