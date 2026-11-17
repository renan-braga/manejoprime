package br.com.curralzap.backend.whatsapp.application;

public class WhatsappOutboundDeliveryException extends RuntimeException {

    public WhatsappOutboundDeliveryException(String message) {
        super(message);
    }

    public WhatsappOutboundDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
