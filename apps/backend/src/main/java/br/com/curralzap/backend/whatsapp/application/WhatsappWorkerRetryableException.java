package br.com.curralzap.backend.whatsapp.application;

import java.util.UUID;

public class WhatsappWorkerRetryableException extends RuntimeException {

    private final UUID inboundMessageId;
    private final String providerMessageId;

    public WhatsappWorkerRetryableException(
            UUID inboundMessageId,
            String providerMessageId,
            String message,
            Throwable cause
    ) {
        super(message, cause);
        this.inboundMessageId = inboundMessageId;
        this.providerMessageId = providerMessageId;
    }

    public UUID inboundMessageId() {
        return inboundMessageId;
    }

    public String providerMessageId() {
        return providerMessageId;
    }
}
