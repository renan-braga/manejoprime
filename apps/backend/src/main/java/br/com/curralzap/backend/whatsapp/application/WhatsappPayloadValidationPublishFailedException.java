package br.com.curralzap.backend.whatsapp.application;

import java.util.UUID;

public class WhatsappPayloadValidationPublishFailedException extends WhatsappWorkerRetryableException {

    public WhatsappPayloadValidationPublishFailedException(
            UUID inboundMessageId,
            String providerMessageId,
            Throwable cause
    ) {
        super(inboundMessageId, providerMessageId, "Failed to publish WhatsApp payload validation request event.", cause);
    }
}
