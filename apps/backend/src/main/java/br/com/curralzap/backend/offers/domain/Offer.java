package br.com.curralzap.backend.offers.domain;

import br.com.curralzap.backend.offers.application.InvalidOfferStateTransitionException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record Offer(
        UUID id,
        UUID farmId,
        String title,
        String notes,
        BigDecimal askingPrice,
        String buyerContact,
        LocalDate expiresAt,
        OfferStatus status,
        List<UUID> animalIds,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static Offer draft(
            UUID farmId,
            String title,
            String notes,
            BigDecimal askingPrice,
            String buyerContact,
            LocalDate expiresAt,
            List<UUID> animalIds
    ) {
        return new Offer(null, farmId, title, notes, askingPrice, buyerContact, expiresAt,
                OfferStatus.DRAFT, List.copyOf(animalIds), null, null);
    }

    public Offer update(
            String title,
            String notes,
            BigDecimal askingPrice,
            String buyerContact,
            LocalDate expiresAt,
            List<UUID> animalIds
    ) {
        return new Offer(id, farmId, title, notes, askingPrice, buyerContact, expiresAt,
                status, List.copyOf(animalIds), createdAt, updatedAt);
    }

    public Offer activate() {
        if (status != OfferStatus.DRAFT) {
            throw new InvalidOfferStateTransitionException(status, OfferStatus.ACTIVE);
        }
        return withStatus(OfferStatus.ACTIVE);
    }

    public Offer cancel() {
        if (status != OfferStatus.DRAFT && status != OfferStatus.ACTIVE) {
            throw new InvalidOfferStateTransitionException(status, OfferStatus.CANCELLED);
        }
        return withStatus(OfferStatus.CANCELLED);
    }

    public Offer markSold() {
        if (status != OfferStatus.ACTIVE) {
            throw new InvalidOfferStateTransitionException(status, OfferStatus.SOLD);
        }
        return withStatus(OfferStatus.SOLD);
    }

    private Offer withStatus(OfferStatus nextStatus) {
        return new Offer(id, farmId, title, notes, askingPrice, buyerContact, expiresAt,
                nextStatus, animalIds, createdAt, updatedAt);
    }
}
