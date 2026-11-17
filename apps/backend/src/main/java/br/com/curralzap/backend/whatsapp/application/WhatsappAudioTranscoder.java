package br.com.curralzap.backend.whatsapp.application;

public interface WhatsappAudioTranscoder {

    TranscodedWhatsappAudio transcode(WhatsappAudioTranscodeRequest request);
}
