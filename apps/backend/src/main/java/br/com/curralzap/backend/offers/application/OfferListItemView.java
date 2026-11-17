package br.com.curralzap.backend.offers.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OfferListItemView(
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
}
