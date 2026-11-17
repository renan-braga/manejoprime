package br.com.curralzap.backend.whatsapp.application;

import java.util.UUID;

public class WhatsappOutboundDeliveryPublishFailedException extends WhatsappWorkerRetryableException {

    public WhatsappOutboundDeliveryPublishFailedException(
            UUID inboundMessageId,
            String providerMessageId,
            Throwable cause
    ) {
        super(inboundMessageId, providerMessageId, "Failed to publish WhatsApp outbound delivery request event.", cause);
    }
}
