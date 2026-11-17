package br.com.curralzap.backend.whatsapp.application;

import java.util.Map;

public class InvalidWhatsappSupportMessageQueryException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public InvalidWhatsappSupportMessageQueryException(Map<String, String> fieldErrors) {
        super("Dados invalidos.");
        this.fieldErrors = fieldErrors;
    }

    public Map<String, String> fieldErrors() {
        return fieldErrors;
    }
}
