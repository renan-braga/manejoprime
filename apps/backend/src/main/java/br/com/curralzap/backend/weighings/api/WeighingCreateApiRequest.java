package br.com.curralzap.backend.weighings.api;

import br.com.curralzap.backend.weighings.application.WeighingCreateCommand;

record WeighingCreateApiRequest(
        String animalId,
        String weightKg,
        String weighingDate,
        String type
) {
    WeighingCreateCommand toCommand() {
        return WeighingCreateCommand.fromApi(animalId, weightKg, weighingDate, type);
    }
}
