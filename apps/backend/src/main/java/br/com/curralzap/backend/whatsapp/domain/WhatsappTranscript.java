package br.com.curralzap.backend.whatsapp.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public record WhatsappTranscript(
        UUID id,
        UUID inboundMessageId,
        String provider,
        String providerModel,
        String transcript,
        OffsetDateTime transcribedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static WhatsappTranscript transcribed(
            UUID inboundMessageId,
            String provider,
            String providerModel,
            String transcript,
            OffsetDateTime transcribedAt
    ) {
        return new WhatsappTranscript(
                null,
                inboundMessageId,
                provider,
                providerModel,
                transcript,
                transcribedAt,
                transcribedAt,
                transcribedAt
        );
    }
}
