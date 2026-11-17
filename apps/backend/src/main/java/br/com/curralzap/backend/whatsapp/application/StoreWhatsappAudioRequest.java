package br.com.curralzap.backend.whatsapp.application;

import java.util.UUID;

public record StoreWhatsappAudioRequest(
        UUID inboundMessageId,
        String providerMessageId,
        String storageKey,
        String mimeType,
        byte[] content
) {
}
