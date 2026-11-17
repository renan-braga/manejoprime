package br.com.curralzap.backend.whatsapp.application;

public interface WhatsappAudioDownloadGateway {

    DownloadedWhatsappAudio download(WhatsappAudioPayload payload);
}
