package br.com.curralzap.backend.whatsapp.application;

public interface WhatsappAudioTranscriber {

    WhatsappAudioTranscriptionResult transcribe(WhatsappAudioTranscriptionRequest request);
}
