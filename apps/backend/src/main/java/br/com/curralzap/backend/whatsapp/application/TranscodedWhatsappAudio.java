package br.com.curralzap.backend.whatsapp.application;

public record TranscodedWhatsappAudio(
        String mimeType,
        byte[] content
) {
}
