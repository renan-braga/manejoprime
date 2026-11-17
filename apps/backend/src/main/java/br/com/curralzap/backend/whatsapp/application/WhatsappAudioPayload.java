package br.com.curralzap.backend.whatsapp.application;

public record WhatsappAudioPayload(
        String mediaUrl,
        String mimeType,
        byte[] rawContent,
        String rawDataJson
) {
    public WhatsappAudioPayload(String mediaUrl, String mimeType) {
        this(mediaUrl, mimeType, null, null);
    }

    public WhatsappAudioPayload(String mediaUrl, String mimeType, byte[] rawContent) {
        this(mediaUrl, mimeType, rawContent, null);
    }
}
