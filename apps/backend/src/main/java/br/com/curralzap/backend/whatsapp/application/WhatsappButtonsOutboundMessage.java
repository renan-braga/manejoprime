package br.com.curralzap.backend.whatsapp.application;

import java.util.List;

public record WhatsappButtonsOutboundMessage(
        String phoneNumber,
        String title,
        String description,
        String footer,
        List<Button> buttons,
        String quotedProviderMessageId
) {
    public record Button(String id, String displayText) {}
}
