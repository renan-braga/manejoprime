package br.com.curralzap.backend.whatsapp.api;

import br.com.curralzap.backend.whatsapp.application.WhatsappSupportMessageListItemView;
import br.com.curralzap.backend.whatsapp.application.WhatsappSupportMessageListView;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record WhatsappSupportMessageListApiResponse(
        List<Item> items,
        long total,
        int limit,
        int offset
) {

    static WhatsappSupportMessageListApiResponse from(WhatsappSupportMessageListView view) {
        return new WhatsappSupportMessageListApiResponse(
                view.items().stream().map(Item::from).toList(),
                view.total(),
                view.limit(),
                view.offset()
        );
    }

    record Item(
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

        static Item from(WhatsappSupportMessageListItemView view) {
            return new Item(
                    view.id(),
                    view.providerMessageId(),
                    view.phoneNumber(),
                    view.correlationId(),
                    view.messageType(),
                    view.status(),
                    view.failureReason(),
                    view.receivedAt(),
                    view.updatedAt()
            );
        }
    }
}
