package br.com.curralzap.backend.feeding.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record FeedingView(
        UUID id,
        UUID animalId,
        UUID lotId,
        String feedType,
        String description,
        BigDecimal quantityKg,
        BigDecimal costPerKg,
        LocalDate startDate,
        LocalDate endDate,
        String source,
        UUID recordedBy
) {
}
