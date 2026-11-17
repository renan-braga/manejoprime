package br.com.curralzap.backend.whatsapp.application;

import java.util.UUID;

public class WhatsappTranscriptExtractionPublishFailedException extends WhatsappWorkerRetryableException {

    public WhatsappTranscriptExtractionPublishFailedException(
            UUID inboundMessageId,
            String providerMessageId,
            Throwable cause
    ) {
        super(inboundMessageId, providerMessageId, "Failed to publish WhatsApp transcript extraction request event.", cause);
    }
}
