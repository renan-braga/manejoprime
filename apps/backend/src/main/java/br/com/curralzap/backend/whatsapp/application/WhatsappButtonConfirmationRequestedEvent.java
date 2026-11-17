package br.com.curralzap.backend.whatsapp.application;

import java.time.OffsetDateTime;
import java.util.UUID;

public record WhatsappButtonConfirmationRequestedEvent(
        int schemaVersion,
        String eventType,
        String eventId,
        String correlationId,
        OffsetDateTime occurredAt,
        UUID inboundMessageId,
        String providerMessageId,
        String selectedButtonId
) implements WhatsappWorkerEvent {
}
