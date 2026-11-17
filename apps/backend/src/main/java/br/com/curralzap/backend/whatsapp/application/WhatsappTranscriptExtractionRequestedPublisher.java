package br.com.curralzap.backend.whatsapp.application;

public interface WhatsappTranscriptExtractionRequestedPublisher {

    void publishRequested(WhatsappTranscriptExtractionRequestedEvent event);
}
