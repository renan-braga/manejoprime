package br.com.curralzap.backend.vaccines.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record Vaccine(
        UUID id,
        UUID farmId,
        UUID animalId,
        UUID lotId,
        String vaccineType,
        String vaccineBatch,
        VaccineDose dose,
        LocalDate applicationDate,
        LocalDate nextDoseDate,
        BigDecimal volumeMl,
        VaccineSource source,
        UUID recordedBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public Vaccine {
        if ((animalId == null) == (lotId == null)) {
            throw new IllegalArgumentException("Exactly one vaccine target must be set");
        }
        if (vaccineType == null || vaccineType.isBlank()) {
            throw new IllegalArgumentException("Vaccine type is required");
        }
    }

    public static Vaccine recordForAnimal(
            UUID farmId,
            UUID animalId,
            UUID recordedBy,
            String vaccineType,
            String vaccineBatch,
            VaccineDose dose,
            LocalDate applicationDate,
            LocalDate nextDoseDate,
            BigDecimal volumeMl,
            VaccineSource source
    ) {
        return new Vaccine(
                null,
                farmId,
                animalId,
                null,
                vaccineType,
                vaccineBatch,
                dose,
                applicationDate,
                nextDoseDate,
                volumeMl,
                source,
                recordedBy,
                null,
                null
        );
    }

    public static Vaccine recordForLot(
            UUID farmId,
            UUID lotId,
            UUID recordedBy,
            String vaccineType,
            String vaccineBatch,
            VaccineDose dose,
            LocalDate applicationDate,
            LocalDate nextDoseDate,
            BigDecimal volumeMl,
            VaccineSource source
    ) {
        return new Vaccine(
                null,
                farmId,
                null,
                lotId,
                vaccineType,
                vaccineBatch,
                dose,
                applicationDate,
                nextDoseDate,
                volumeMl,
                source,
                recordedBy,
                null,
                null
        );
    }
}
