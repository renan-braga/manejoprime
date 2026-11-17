package br.com.curralzap.backend.animals.api;

import br.com.curralzap.backend.animals.application.AnimalDetailView;
import br.com.curralzap.backend.animals.application.AnimalLotView;
import br.com.curralzap.backend.animals.application.AnimalSummaryView;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

record AnimalDetailApiResponse(
        UUID id,
        String identifier,
        String name,
        String sex,
        String breed,
        String status,
        LocalDate birthDate,
        LocalDate entryDate,
        UUID farmId,
        AnimalLotApiResponse lot,
        AnimalSummaryApiResponse summary
) {

    static AnimalDetailApiResponse from(AnimalDetailView animal) {
        return new AnimalDetailApiResponse(
                animal.id(),
                animal.identifier(),
                animal.name(),
                animal.sex(),
                animal.breed(),
                animal.status(),
                animal.birthDate(),
                animal.entryDate(),
                animal.farmId(),
                animal.lot() != null ? AnimalLotApiResponse.from(animal.lot()) : null,
                AnimalSummaryApiResponse.from(animal.summary())
        );
    }

    record AnimalLotApiResponse(
            UUID id,
            String name
    ) {
        static AnimalLotApiResponse from(AnimalLotView lot) {
            return new AnimalLotApiResponse(lot.id(), lot.name());
        }
    }

    record AnimalSummaryApiResponse(
            BigDecimal birthWeightKg,
            Long ageInDays,
            Long daysOnFarm
    ) {
        static AnimalSummaryApiResponse from(AnimalSummaryView summary) {
            return new AnimalSummaryApiResponse(
                    summary.birthWeightKg(),
                    summary.ageInDays(),
                    summary.daysOnFarm()
            );
        }
    }
}
