package br.com.curralzap.backend.vaccines.api;

import br.com.curralzap.backend.vaccines.application.VaccineView;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

record VaccineApiResponse(
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
    static VaccineApiResponse from(VaccineView vaccine) {
        return new VaccineApiResponse(
                vaccine.id(),
                vaccine.animalId(),
                vaccine.lotId(),
                vaccine.vaccineType(),
                vaccine.vaccineBatch(),
                vaccine.dose(),
                vaccine.applicationDate(),
                vaccine.nextDoseDate(),
                vaccine.volumeMl(),
                vaccine.source(),
                vaccine.recordedBy()
        );
    }
}
