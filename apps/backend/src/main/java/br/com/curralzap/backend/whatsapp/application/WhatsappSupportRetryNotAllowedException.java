package br.com.curralzap.backend.whatsapp.application;

public class WhatsappSupportRetryNotAllowedException extends RuntimeException {

    public WhatsappSupportRetryNotAllowedException(String message) {
        super(message);
    }
}
