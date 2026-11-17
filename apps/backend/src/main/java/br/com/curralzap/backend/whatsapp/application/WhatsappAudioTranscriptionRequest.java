package br.com.curralzap.backend.whatsapp.application;

public record WhatsappAudioTranscriptionRequest(
        String providerMessageId,
        String mimeType,
        byte[] content
) {
}
