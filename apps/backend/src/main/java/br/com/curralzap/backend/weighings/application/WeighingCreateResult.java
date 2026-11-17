package br.com.curralzap.backend.weighings.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record WeighingCreateResult(
        UUID id,
        UUID animalId,
        BigDecimal weightKg,
        LocalDate weighingDate,
        String type,
        String source,
        UUID recordedBy,
        BigDecimal gmdKgPerDay
) {
}
