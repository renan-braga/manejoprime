package br.com.curralzap.backend.whatsapp.application;

import java.util.UUID;

public class WhatsappMessagePublishFailedException extends WhatsappWorkerRetryableException {

    public WhatsappMessagePublishFailedException(UUID inboundMessageId, String providerMessageId, Throwable cause) {
        super(inboundMessageId, providerMessageId, "Failed to publish received WhatsApp message event.", cause);
    }
}
