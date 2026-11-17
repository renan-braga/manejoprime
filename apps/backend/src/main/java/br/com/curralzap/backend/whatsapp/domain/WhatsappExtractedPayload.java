package br.com.curralzap.backend.whatsapp.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public record WhatsappExtractedPayload(
        UUID id,
        UUID inboundMessageId,
        String provider,
        String providerModel,
        String intent,
        String structuredPayload,
        OffsetDateTime extractedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static WhatsappExtractedPayload extracted(
            UUID inboundMessageId,
            String provider,
            String providerModel,
            String intent,
            String structuredPayload,
            OffsetDateTime extractedAt
    ) {
        return new WhatsappExtractedPayload(
                null,
                inboundMessageId,
                provider,
                providerModel,
                intent,
                structuredPayload,
                extractedAt,
                extractedAt,
                extractedAt
        );
    }
}
