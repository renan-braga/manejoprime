package br.com.curralzap.backend.whatsapp.application;

public record WhatsappTranscriptExtractionRequest(
        String providerMessageId,
        String transcript
) {
}
