package br.com.curralzap.backend.offers.api;

import br.com.curralzap.backend.offers.application.OfferDetailView;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

record OfferApiResponse(
        UUID id,
        UUID farmId,
        String title,
        String notes,
        BigDecimal askingPrice,
        String buyerContact,
        LocalDate expiresAt,
        String status,
        List<UUID> animalIds,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    static OfferApiResponse from(OfferDetailView view) {
        return new OfferApiResponse(
                view.id(),
                view.farmId(),
                view.title(),
                view.notes(),
                view.askingPrice(),
                view.buyerContact(),
                view.expiresAt(),
                view.status(),
                view.animalIds(),
                view.createdAt(),
                view.updatedAt()
        );
    }
}
