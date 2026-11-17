package br.com.curralzap.backend.whatsapp.application;

public interface WhatsappAudioStorage {

    String store(StoreWhatsappAudioRequest request);

    byte[] load(String storagePath);
}
