package br.com.curralzap.backend.feeding.api;

import br.com.curralzap.backend.feeding.application.FeedingCreateCommand;

record FeedingCreateApiRequest(
        String animalId,
        String lotId,
        String feedType,
        String description,
        String quantityKg,
        String costPerKg,
        String startDate,
        String endDate
) {
    FeedingCreateCommand toCommand() {
        return FeedingCreateCommand.fromApi(
                animalId,
                lotId,
                feedType,
                description,
                quantityKg,
                costPerKg,
                startDate,
                endDate
        );
    }
}
