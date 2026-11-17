package br.com.curralzap.backend.offers.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OfferDetailView(
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
}
