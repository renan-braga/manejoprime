package br.com.curralzap.backend.whatsapp.application;

public record WhatsappWebhookIntakeCommand(
        String eventType,
        String providerMessageId,
        String remoteJid,
        String messageType,
        String rawPayload,
        String selectedButtonId
) {
}
