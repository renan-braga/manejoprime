package br.com.curralzap.backend.whatsapp.application;

import java.time.OffsetDateTime;
import java.util.UUID;

public record WhatsappSupportMessageListItemView(
        UUID id,
        String providerMessageId,
        String phoneNumber,
        String correlationId,
        String messageType,
        String status,
        String failureReason,
        OffsetDateTime receivedAt,
        OffsetDateTime updatedAt
) {
}
