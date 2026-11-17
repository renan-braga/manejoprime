package br.com.curralzap.backend.animals.domain;

import br.com.curralzap.backend.animals.application.AnimalMutationCommand;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record Animal(
        UUID id,
        UUID farmId,
        UUID lotId,
        String identifier,
        String name,
        String breed,
        AnimalSex sex,
        LocalDate birthDate,
        BigDecimal birthWeightKg,
        UUID damId,
        UUID sireId,
        AnimalStatus status,
        LocalDate entryDate,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static Animal create(UUID farmId, AnimalMutationCommand command) {
        return new Animal(
                null,
                farmId,
                command.lotId(),
                command.identifier(),
                command.name(),
                command.breed(),
                command.sex(),
                command.birthDate(),
                command.birthWeightKg(),
                null,
                null,
                command.status(),
                command.entryDate(),
                null,
                null
        );
    }

    public Animal update(AnimalMutationCommand command) {
        return new Animal(
                id,
                farmId,
                command.lotId(),
                command.identifier(),
                command.name(),
                command.breed(),
                command.sex(),
                command.birthDate(),
                command.birthWeightKg(),
                damId,
                sireId,
                command.status(),
                command.entryDate(),
                createdAt,
                updatedAt
        );
    }
}
