package br.com.curralzap.backend.whatsapp.application;

public record WhatsappSupportRetryView(
        boolean supported,
        String action,
        String reason
) {
}
