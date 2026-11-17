package br.com.curralzap.backend.whatsapp.application;

import br.com.curralzap.backend.whatsapp.domain.WhatsappProcessingStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record WhatsappOutboundDeliveryRequestedEvent(
        int schemaVersion,
        String eventType,
        String eventId,
        String correlationId,
        OffsetDateTime occurredAt,
        UUID inboundMessageId,
        String providerMessageId,
        WhatsappProcessingStatus status
) implements WhatsappWorkerEvent {
}
