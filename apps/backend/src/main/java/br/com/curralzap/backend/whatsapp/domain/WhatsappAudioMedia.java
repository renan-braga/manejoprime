package br.com.curralzap.backend.whatsapp.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public record WhatsappAudioMedia(
        UUID id,
        UUID inboundMessageId,
        String sourceUrl,
        String mimeType,
        String storagePath,
        OffsetDateTime downloadedAt,
        String transcodedMimeType,
        String transcodedStoragePath,
        OffsetDateTime transcodedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static WhatsappAudioMedia downloaded(
            UUID inboundMessageId,
            String sourceUrl,
            String mimeType,
            String storagePath,
            OffsetDateTime downloadedAt
    ) {
        return new WhatsappAudioMedia(
                null,
                inboundMessageId,
                sourceUrl,
                mimeType,
                storagePath,
                downloadedAt,
                null,
                null,
                null,
                downloadedAt,
                downloadedAt
        );
    }

    public WhatsappAudioMedia markTranscoded(
            String transcodedMimeType,
            String transcodedStoragePath,
            OffsetDateTime transcodedAt
    ) {
        return new WhatsappAudioMedia(
                id,
                inboundMessageId,
                sourceUrl,
                mimeType,
                storagePath,
                downloadedAt,
                transcodedMimeType,
                transcodedStoragePath,
                transcodedAt,
                createdAt,
                transcodedAt
        );
    }
}
