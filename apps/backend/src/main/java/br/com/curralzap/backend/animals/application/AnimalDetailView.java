package br.com.curralzap.backend.animals.application;

import java.time.LocalDate;
import java.util.UUID;

public record AnimalDetailView(
        UUID id,
        String identifier,
        String name,
        String sex,
        String breed,
        String status,
        LocalDate birthDate,
        LocalDate entryDate,
        UUID farmId,
        AnimalLotView lot,
        AnimalSummaryView summary
) {
}
