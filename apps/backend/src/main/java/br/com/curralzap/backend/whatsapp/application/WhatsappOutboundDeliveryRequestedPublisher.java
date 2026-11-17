package br.com.curralzap.backend.whatsapp.application;

public interface WhatsappOutboundDeliveryRequestedPublisher {

    void publishRequested(WhatsappOutboundDeliveryRequestedEvent event);
}
