package br.com.curralzap.backend.whatsapp.application;

public interface WhatsappAudioTranscriptionRequestedPublisher {

    void publishRequested(WhatsappAudioTranscriptionRequestedEvent event);
}
