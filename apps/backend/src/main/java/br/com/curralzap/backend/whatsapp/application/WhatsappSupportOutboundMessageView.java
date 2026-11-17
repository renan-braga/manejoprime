package br.com.curralzap.backend.whatsapp.application;

import java.time.OffsetDateTime;
import java.util.UUID;

public record WhatsappSupportOutboundMessageView(
        UUID id,
        String status,
        String payload,
        String providerMessageId,
        String failureReason,
        OffsetDateTime queuedAt,
        OffsetDateTime sentAt,
        OffsetDateTime updatedAt
) {
}
