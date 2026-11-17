package br.com.curralzap.backend.whatsapp.application;

import java.util.UUID;

public class WhatsappAudioTranscodePublishFailedException extends WhatsappWorkerRetryableException {

    public WhatsappAudioTranscodePublishFailedException(
            UUID inboundMessageId,
            String providerMessageId,
            Throwable cause
    ) {
        super(inboundMessageId, providerMessageId, "Failed to publish WhatsApp audio transcode request event.", cause);
    }
}
