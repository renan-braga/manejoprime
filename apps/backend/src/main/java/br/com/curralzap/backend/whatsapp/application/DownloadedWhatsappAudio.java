package br.com.curralzap.backend.whatsapp.application;

public record DownloadedWhatsappAudio(
        String contentType,
        byte[] content
) {
}
