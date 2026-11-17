package br.com.curralzap.backend.whatsapp.application;

public record WhatsappAudioTranscodeRequest(
        String providerMessageId,
        String sourceMimeType,
        byte[] content
) {
}
