package br.com.curralzap.backend.whatsapp.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public record WhatsappInboundMessage(
        UUID id,
        String providerMessageId,
        String eventType,
        String phoneNumber,
        UUID farmId,
        UUID userId,
        String correlationId,
        String messageType,
        String rawPayload,
        WhatsappProcessingStatus status,
        String failureReason,
        OffsetDateTime receivedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static WhatsappInboundMessage receive(
            String providerMessageId,
            String eventType,
            String phoneNumber,
            UUID farmId,
            UUID userId,
            String messageType,
            String rawPayload,
            OffsetDateTime receivedAt
    ) {
        return receive(
                providerMessageId,
                eventType,
                phoneNumber,
                farmId,
                userId,
                null,
                messageType,
                rawPayload,
                receivedAt
        );
    }

    public static WhatsappInboundMessage receive(
            String providerMessageId,
            String eventType,
            String phoneNumber,
            UUID farmId,
            UUID userId,
            String correlationId,
            String messageType,
            String rawPayload,
            OffsetDateTime receivedAt
    ) {
        return new WhatsappInboundMessage(
                null,
                providerMessageId,
                eventType,
                phoneNumber,
                farmId,
                userId,
                correlationId,
                messageType,
                rawPayload,
                WhatsappProcessingStatus.RECEIVED,
                null,
                receivedAt,
                receivedAt,
                receivedAt
        );
    }

    public WhatsappInboundMessage withProcessingStatus(
            WhatsappProcessingStatus status,
            String failureReason,
            OffsetDateTime updatedAt
    ) {
        return new WhatsappInboundMessage(
                id,
                providerMessageId,
                eventType,
                phoneNumber,
                farmId,
                userId,
                correlationId,
                messageType,
                rawPayload,
                status,
                failureReason,
                receivedAt,
                createdAt,
                updatedAt
        );
    }
}
