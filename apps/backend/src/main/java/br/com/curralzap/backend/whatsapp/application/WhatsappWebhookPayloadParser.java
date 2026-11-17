package br.com.curralzap.backend.whatsapp.application;

public interface WhatsappWebhookPayloadParser {

    WhatsappWebhookIntakeCommand parse(String rawPayload);
}
