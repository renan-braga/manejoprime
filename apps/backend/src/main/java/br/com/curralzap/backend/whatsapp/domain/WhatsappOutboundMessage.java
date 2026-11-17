package br.com.curralzap.backend.whatsapp.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public record WhatsappOutboundMessage(
        UUID id,
        UUID inboundMessageId,
        UUID farmId,
        UUID userId,
        String correlationId,
        String phoneNumber,
        String messageType,
        String payload,
        String providerMessageId,
        WhatsappOutboundStatus status,
        String failureReason,
        OffsetDateTime queuedAt,
        OffsetDateTime sentAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static WhatsappOutboundMessage queue(
            UUID inboundMessageId,
            UUID farmId,
            UUID userId,
            String correlationId,
            String phoneNumber,
            String messageType,
            String payload,
            OffsetDateTime queuedAt
    ) {
        return new WhatsappOutboundMessage(
                null,
                inboundMessageId,
                farmId,
                userId,
                correlationId,
                phoneNumber,
                messageType,
                payload,
                null,
                WhatsappOutboundStatus.QUEUED,
                null,
                queuedAt,
                null,
                queuedAt,
                queuedAt
        );
    }

    public WhatsappOutboundMessage markSent(String providerMessageId, OffsetDateTime sentAt) {
        return new WhatsappOutboundMessage(
                id,
                inboundMessageId,
                farmId,
                userId,
                correlationId,
                phoneNumber,
                messageType,
                payload,
                providerMessageId,
                WhatsappOutboundStatus.SENT,
                null,
                queuedAt,
                sentAt,
                createdAt,
                sentAt
        );
    }

    public WhatsappOutboundMessage markFailed(String failureReason, OffsetDateTime failedAt) {
        return new WhatsappOutboundMessage(
                id,
                inboundMessageId,
                farmId,
                userId,
                correlationId,
                phoneNumber,
                messageType,
                payload,
                providerMessageId,
                WhatsappOutboundStatus.FAILED,
                failureReason,
                queuedAt,
                sentAt,
                createdAt,
                failedAt
        );
    }
}
