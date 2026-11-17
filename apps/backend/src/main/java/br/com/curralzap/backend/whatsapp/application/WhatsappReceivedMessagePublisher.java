package br.com.curralzap.backend.whatsapp.application;

public interface WhatsappReceivedMessagePublisher {

    void publishReceived(WhatsappMessageReceivedEvent event);
}
