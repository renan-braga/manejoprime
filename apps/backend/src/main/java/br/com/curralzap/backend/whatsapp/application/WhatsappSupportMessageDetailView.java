package br.com.curralzap.backend.whatsapp.application;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record WhatsappSupportMessageDetailView(
        UUID id,
        String providerMessageId,
        String eventType,
        String phoneNumber,
        UUID farmId,
        UUID userId,
        String correlationId,
        String messageType,
        String rawPayload,
        String status,
        String failureReason,
        OffsetDateTime receivedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        WhatsappSupportRetryView retry,
        List<WhatsappSupportProcessingStepView> processingSteps,
        WhatsappSupportTranscriptView transcript,
        WhatsappSupportExtractedPayloadView extractedPayload,
        List<WhatsappSupportOutboundMessageView> outboundMessages
) {
}
