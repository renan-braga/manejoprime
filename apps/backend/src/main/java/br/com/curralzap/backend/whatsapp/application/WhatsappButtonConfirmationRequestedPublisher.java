package br.com.curralzap.backend.whatsapp.application;

public interface WhatsappButtonConfirmationRequestedPublisher {

    void publishRequested(WhatsappButtonConfirmationRequestedEvent event);
}
