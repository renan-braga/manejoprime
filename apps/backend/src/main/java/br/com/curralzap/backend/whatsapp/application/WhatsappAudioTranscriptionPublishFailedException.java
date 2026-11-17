package br.com.curralzap.backend.whatsapp.application;

import java.util.UUID;

public class WhatsappAudioTranscriptionPublishFailedException extends WhatsappWorkerRetryableException {

    public WhatsappAudioTranscriptionPublishFailedException(
            UUID inboundMessageId,
            String providerMessageId,
            Throwable cause
    ) {
        super(inboundMessageId, providerMessageId, "Failed to publish WhatsApp audio transcription request event.", cause);
    }
}
