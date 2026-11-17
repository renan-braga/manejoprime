package br.com.curralzap.backend.animals.api;

import br.com.curralzap.backend.animals.application.AnimalMutationResult;

import java.util.UUID;

record AnimalMutationApiResponse(
        UUID id
) {
    static AnimalMutationApiResponse from(AnimalMutationResult result) {
        return new AnimalMutationApiResponse(result.id());
    }
}
