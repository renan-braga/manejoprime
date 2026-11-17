package br.com.curralzap.backend.whatsapp.application;

public interface WhatsappAudioTranscodeRequestedPublisher {

    void publishRequested(WhatsappAudioTranscodeRequestedEvent event);
}
