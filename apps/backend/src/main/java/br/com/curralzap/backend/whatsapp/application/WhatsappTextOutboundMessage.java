package br.com.curralzap.backend.whatsapp.application;

public record WhatsappTextOutboundMessage(
        String phoneNumber,
        String text,
        String quotedProviderMessageId
) {
}
