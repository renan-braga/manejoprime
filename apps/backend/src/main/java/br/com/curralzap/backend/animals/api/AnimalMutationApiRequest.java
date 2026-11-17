package br.com.curralzap.backend.animals.api;

import br.com.curralzap.backend.animals.application.AnimalMutationCommand;

record AnimalMutationApiRequest(
        String identifier,
        String name,
        String breed,
        String sex,
        String birthDate,
        String birthWeightKg,
        String status,
        String entryDate,
        String lotId
) {
    AnimalMutationCommand toCommand() {
        return AnimalMutationCommand.fromApi(
                identifier,
                name,
                breed,
                sex,
                birthDate,
                birthWeightKg,
                status,
                entryDate,
                lotId
        );
    }
}
