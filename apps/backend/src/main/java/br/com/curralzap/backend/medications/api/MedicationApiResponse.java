package br.com.curralzap.backend.medications.api;

import br.com.curralzap.backend.medications.application.MedicationView;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

record MedicationApiResponse(
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
    static MedicationApiResponse from(MedicationView medication) {
        return new MedicationApiResponse(
                medication.id(),
                medication.animalId(),
                medication.occurrenceId(),
                medication.medicationName(),
                medication.activeIngredient(),
                medication.reason(),
                medication.dosage(),
                medication.withdrawalPeriodDays(),
                medication.applicationDate(),
                medication.releaseDate(),
                medication.inWithdrawalPeriod(),
                medication.cost(),
                medication.source(),
                medication.recordedBy()
        );
    }
}
