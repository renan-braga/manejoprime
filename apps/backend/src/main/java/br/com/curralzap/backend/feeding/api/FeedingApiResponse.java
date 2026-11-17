package br.com.curralzap.backend.feeding.api;

import br.com.curralzap.backend.feeding.application.FeedingView;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

record FeedingApiResponse(
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
    static FeedingApiResponse from(FeedingView feeding) {
        return new FeedingApiResponse(
                feeding.id(),
                feeding.animalId(),
                feeding.lotId(),
                feeding.feedType(),
                feeding.description(),
                feeding.quantityKg(),
                feeding.costPerKg(),
                feeding.startDate(),
                feeding.endDate(),
                feeding.source(),
                feeding.recordedBy()
        );
    }
}
