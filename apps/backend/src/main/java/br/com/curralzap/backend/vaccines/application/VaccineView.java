package br.com.curralzap.backend.vaccines.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record VaccineView(
        UUID id,
        UUID animalId,
        UUID lotId,
        String vaccineType,
        String vaccineBatch,
        String dose,
        LocalDate applicationDate,
        LocalDate nextDoseDate,
        BigDecimal volumeMl,
        String source,
        UUID recordedBy
) {
}
