package br.com.curralzap.backend.weighings.api;

import br.com.curralzap.backend.weighings.application.WeighingCreateResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

record WeighingCreateApiResponse(
        UUID id,
        UUID animalId,
        BigDecimal weightKg,
        LocalDate weighingDate,
        String type,
        String source,
        UUID recordedBy,
        BigDecimal gmdKgPerDay
) {
    static WeighingCreateApiResponse from(WeighingCreateResult result) {
        return new WeighingCreateApiResponse(
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
}
