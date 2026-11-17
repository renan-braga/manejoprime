package br.com.curralzap.backend.weighings.api;

import br.com.curralzap.backend.weighings.application.WeighingCreateResult;
import br.com.curralzap.backend.weighings.application.WeighingHistoryItemView;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

record WeighingApiResponse(
        UUID id,
        UUID animalId,
        BigDecimal weightKg,
        LocalDate weighingDate,
        String type,
        String source,
        UUID recordedBy,
        BigDecimal gmdKgPerDay
) {
    static WeighingApiResponse from(WeighingCreateResult result) {
        return new WeighingApiResponse(
                result.id(),
                result.animalId(),
                result.weightKg(),
                result.weighingDate(),
                result.type(),
                result.source(),
                result.recordedBy(),
                result.gmdKgPerDay()
        );
    }

    static WeighingApiResponse from(WeighingHistoryItemView weighing) {
        return new WeighingApiResponse(
                weighing.id(),
                weighing.animalId(),
                weighing.weightKg(),
                weighing.weighingDate(),
                weighing.type(),
                weighing.source(),
                weighing.recordedBy(),
                weighing.gmdKgPerDay()
        );
    }
}
