package br.com.curralzap.backend.whatsapp.application;

public interface WhatsappPayloadValidationRequestedPublisher {

    void publishRequested(WhatsappPayloadValidationRequestedEvent event);
}
