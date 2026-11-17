package br.com.curralzap.backend.occurrences.application;

import br.com.curralzap.backend.occurrences.domain.OccurrenceSeverity;
import br.com.curralzap.backend.occurrences.domain.OccurrenceType;

import java.util.UUID;

public record OccurrenceCreateCommand(
        UUID animalId,
        OccurrenceType type,
        OccurrenceSeverity severity,
        String description
) {
    public static OccurrenceCreateCommand fromApi(String animalId, String type, String severity, String description) {
        return new OccurrenceCreateCommand(
                UUID.fromString(animalId),
                OccurrenceType.fromDatabaseValue(type),
                OccurrenceSeverity.fromDatabaseValue(severity),
                description
        );
    }
}
