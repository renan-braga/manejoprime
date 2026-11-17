package br.com.curralzap.backend.whatsapp.application;

import java.util.UUID;

public class WhatsappButtonConfirmationPublishFailedException extends RuntimeException {

    private final UUID inboundMessageId;
    private final String providerMessageId;

    public WhatsappButtonConfirmationPublishFailedException(
            UUID inboundMessageId,
            String providerMessageId,
            Exception cause
    ) {
        super("Failed to publish button confirmation event for message " + providerMessageId, cause);
        this.inboundMessageId = inboundMessageId;
        this.providerMessageId = providerMessageId;
    }

    public UUID getInboundMessageId() {
        return inboundMessageId;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }
}
