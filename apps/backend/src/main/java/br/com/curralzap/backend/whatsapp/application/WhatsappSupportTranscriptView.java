package br.com.curralzap.backend.whatsapp.application;

import java.time.OffsetDateTime;
import java.util.UUID;

public record WhatsappSupportTranscriptView(
        UUID id,
        String provider,
        String providerModel,
        String transcript,
        OffsetDateTime transcribedAt
) {
}
