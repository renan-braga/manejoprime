package br.com.curralzap.backend.whatsapp.application;

public record WhatsappAudioTranscriptionResult(
        String provider,
        String providerModel,
        String transcript
) {
}
