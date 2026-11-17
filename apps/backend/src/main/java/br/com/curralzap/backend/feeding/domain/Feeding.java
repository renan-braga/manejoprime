package br.com.curralzap.backend.feeding.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record Feeding(
        UUID id,
        UUID farmId,
        UUID animalId,
        UUID lotId,
        FeedingType feedType,
        String description,
        BigDecimal quantityKg,
        BigDecimal costPerKg,
        LocalDate startDate,
        LocalDate endDate,
        FeedingSource source,
        UUID recordedBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public Feeding {
        if ((animalId == null) == (lotId == null)) {
            throw new IllegalArgumentException("Exactly one feeding target must be set");
        }
        if (feedType == null) {
            throw new IllegalArgumentException("Feeding type is required");
        }
        if (quantityKg == null || quantityKg.signum() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        if (costPerKg != null && costPerKg.signum() < 0) {
            throw new IllegalArgumentException("Cost per kg must be zero or greater");
        }
        if (startDate == null) {
            throw new IllegalArgumentException("Start date is required");
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must be on or after start date");
        }
        if (source == null) {
            throw new IllegalArgumentException("Feeding source is required");
        }
        if (recordedBy == null) {
            throw new IllegalArgumentException("Recorded by is required");
        }
    }

    public static Feeding recordForAnimal(
            UUID farmId,
            UUID animalId,
            UUID recordedBy,
            FeedingType feedType,
            String description,
            BigDecimal quantityKg,
            BigDecimal costPerKg,
            LocalDate startDate,
            LocalDate endDate,
            FeedingSource source
    ) {
        return new Feeding(
                null,
                farmId,
                animalId,
                null,
                feedType,
                description,
                quantityKg,
                costPerKg,
                startDate,
                endDate,
                source,
                recordedBy,
                null,
                null
        );
    }

    public static Feeding recordForLot(
            UUID farmId,
            UUID lotId,
            UUID recordedBy,
            FeedingType feedType,
            String description,
            BigDecimal quantityKg,
            BigDecimal costPerKg,
            LocalDate startDate,
            LocalDate endDate,
            FeedingSource source
    ) {
        return new Feeding(
                null,
                farmId,
                null,
                lotId,
                feedType,
                description,
                quantityKg,
                costPerKg,
                startDate,
                endDate,
                source,
                recordedBy,
                null,
                null
        );
    }

    public boolean isActiveOn(LocalDate referenceDate) {
        return (startDate.isEqual(referenceDate) || startDate.isBefore(referenceDate))
                && (endDate == null || endDate.isEqual(referenceDate) || endDate.isAfter(referenceDate));
    }
}
