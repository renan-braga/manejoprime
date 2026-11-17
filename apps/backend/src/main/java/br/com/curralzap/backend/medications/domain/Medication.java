package br.com.curralzap.backend.medications.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record Medication(
        UUID id,
        UUID farmId,
        UUID animalId,
        UUID occurrenceId,
        String medicationName,
        String activeIngredient,
        String reason,
        String dosage,
        LocalDate applicationDate,
        int withdrawalPeriodDays,
        LocalDate releaseDate,
        BigDecimal cost,
        MedicationSource source,
        UUID recordedBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public Medication {
        if (animalId == null) {
            throw new IllegalArgumentException("Animal id is required");
        }
        if (medicationName == null || medicationName.isBlank()) {
            throw new IllegalArgumentException("Medication name is required");
        }
        if (dosage == null || dosage.isBlank()) {
            throw new IllegalArgumentException("Dosage is required");
        }
        if (applicationDate == null) {
            throw new IllegalArgumentException("Application date is required");
        }
        if (withdrawalPeriodDays < 0) {
            throw new IllegalArgumentException("Withdrawal period must be zero or greater");
        }
        if (releaseDate == null) {
            throw new IllegalArgumentException("Release date is required");
        }
    }

    public static Medication record(
            UUID farmId,
            UUID animalId,
            UUID occurrenceId,
            UUID recordedBy,
            String medicationName,
            String activeIngredient,
            String reason,
            String dosage,
            LocalDate applicationDate,
            int withdrawalPeriodDays,
            BigDecimal cost,
            MedicationSource source
    ) {
        return new Medication(
                null,
                farmId,
                animalId,
                occurrenceId,
                medicationName,
                activeIngredient,
                reason,
                dosage,
                applicationDate,
                withdrawalPeriodDays,
                applicationDate.plusDays(withdrawalPeriodDays),
                cost,
                source,
                recordedBy,
                null,
                null
        );
    }

    public boolean inWithdrawalPeriodOn(LocalDate referenceDate) {
        return (applicationDate.isEqual(referenceDate) || applicationDate.isBefore(referenceDate))
                && (releaseDate.isEqual(referenceDate) || releaseDate.isAfter(referenceDate));
    }
}
