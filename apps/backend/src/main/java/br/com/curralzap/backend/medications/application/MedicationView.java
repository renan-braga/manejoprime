package br.com.curralzap.backend.medications.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record MedicationView(
        UUID id,
        UUID animalId,
        UUID occurrenceId,
        String medicationName,
        String activeIngredient,
        String reason,
        String dosage,
        int withdrawalPeriodDays,
        LocalDate applicationDate,
        LocalDate releaseDate,
        boolean inWithdrawalPeriod,
        BigDecimal cost,
        String source,
        UUID recordedBy
) {
}
