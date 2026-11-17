package br.com.curralzap.backend.whatsapp.application;

import br.com.curralzap.backend.whatsapp.domain.WhatsappProcessingStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record WhatsappMessageReceivedEvent(
        int schemaVersion,
        String eventType,
        String eventId,
        String correlationId,
        OffsetDateTime occurredAt,
        UUID inboundMessageId,
        String providerMessageId,
        UUID farmId,
        UUID userId,
        String phoneNumber,
        String messageType,
        WhatsappProcessingStatus processingStatus
) implements WhatsappWorkerEvent {
}
