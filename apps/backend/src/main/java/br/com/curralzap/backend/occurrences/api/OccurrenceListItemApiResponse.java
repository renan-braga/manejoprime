package br.com.curralzap.backend.occurrences.api;

import br.com.curralzap.backend.occurrences.application.OccurrenceListItemView;

import java.time.OffsetDateTime;
import java.util.UUID;

record OccurrenceListItemApiResponse(
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
    static OccurrenceListItemApiResponse from(OccurrenceListItemView item) {
        return new OccurrenceListItemApiResponse(
                item.id(),
                item.animalId(),
                item.animalIdentifier(),
                item.animalName(),
                item.type(),
                item.severity(),
                item.status(),
                item.description(),
                item.photoUrl(),
                item.source(),
                item.recordedBy(),
                item.resolvedBy(),
                item.createdAt(),
                item.updatedAt()
        );
    }
}
