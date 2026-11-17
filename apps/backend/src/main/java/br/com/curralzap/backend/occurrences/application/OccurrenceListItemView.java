package br.com.curralzap.backend.occurrences.application;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OccurrenceListItemView(
        UUID id,
        UUID animalId,
        String animalIdentifier,
        String animalName,
        String type,
        String severity,
        String status,
        String description,
        String photoUrl,
        String source,
        UUID recordedBy,
        UUID resolvedBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
