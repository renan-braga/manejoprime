package br.com.curralzap.backend.animals.application;

import java.util.Optional;
import java.util.UUID;

public interface AnimalReadViewRepository {

    AnimalListView listFor(UUID farmId, AnimalListQuery query);

    Optional<AnimalDetailView> findDetailById(UUID farmId, UUID animalId);
}
