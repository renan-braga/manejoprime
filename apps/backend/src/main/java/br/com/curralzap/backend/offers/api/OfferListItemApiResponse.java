package br.com.curralzap.backend.offers.api;

import br.com.curralzap.backend.offers.application.OfferListItemView;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

record OfferListItemApiResponse(
        UUID id,
        UUID farmId,
        String title,
        BigDecimal askingPrice,
        String buyerContact,
        LocalDate expiresAt,
        String status,
        int animalCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    static OfferListItemApiResponse from(OfferListItemView view) {
        return new OfferListItemApiResponse(
                view.id(),
                view.farmId(),
                view.title(),
                view.askingPrice(),
                view.buyerContact(),
                view.expiresAt(),
                view.status(),
                view.animalCount(),
                view.createdAt(),
                view.updatedAt()
        );
    }
}
