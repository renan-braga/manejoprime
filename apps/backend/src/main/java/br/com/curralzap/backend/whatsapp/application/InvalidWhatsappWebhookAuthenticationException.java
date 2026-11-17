package br.com.curralzap.backend.whatsapp.application;

public class InvalidWhatsappWebhookAuthenticationException extends RuntimeException {

    public InvalidWhatsappWebhookAuthenticationException() {
        super("Webhook authentication failed.");
    }
}
