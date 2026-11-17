package br.com.curralzap.backend.animals.api;

import br.com.curralzap.backend.animals.application.AnimalListItemView;
import br.com.curralzap.backend.animals.application.AnimalListView;

import java.util.List;
import java.util.UUID;

record AnimalListApiResponse(
        List<AnimalListItemApiResponse> items,
        long total,
        int limit,
        int offset
) {

    static AnimalListApiResponse from(AnimalListView animalList) {
        return new AnimalListApiResponse(
                animalList.items().stream().map(AnimalListItemApiResponse::from).toList(),
                animalList.total(),
                animalList.limit(),
                animalList.offset()
        );
    }

    record AnimalListItemApiResponse(
            UUID id,
            String identifier,
            String name,
            String sex,
            String breed,
            String status,
            UUID lotId,
            String lotName
    ) {
        static AnimalListItemApiResponse from(AnimalListItemView animal) {
            return new AnimalListItemApiResponse(
                    animal.id(),
                    animal.identifier(),
                    animal.name(),
                    animal.sex(),
                    animal.breed(),
                    animal.status(),
                    animal.lotId(),
                    animal.lotName()
            );
        }
    }
}
