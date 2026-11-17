package br.com.curralzap.backend.whatsapp.application;

import java.util.Map;

public class InvalidWhatsappWebhookPayloadException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public InvalidWhatsappWebhookPayloadException(Map<String, String> fieldErrors) {
        super("Dados inválidos.");
        this.fieldErrors = fieldErrors;
    }

    public Map<String, String> fieldErrors() {
        return fieldErrors;
    }
}
