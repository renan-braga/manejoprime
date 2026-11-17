package br.com.curralzap.backend.animals.application;

import java.util.UUID;

public record AnimalListItemView(
        UUID id,
        String identifier,
        String name,
        String sex,
        String breed,
        String status,
        UUID lotId,
        String lotName
) {
}
