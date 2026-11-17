package br.com.curralzap.backend.occurrences.application;

import br.com.curralzap.backend.occurrences.domain.Occurrence;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OccurrenceView(
        UUID id,
        UUID animalId,
        String type,
        String severity,
        String status,
        String description,
        String source,
        UUID recordedBy,
        OffsetDateTime createdAt
) {
    public static OccurrenceView from(Occurrence occurrence) {
        return new OccurrenceView(
                occurrence.id(),
                occurrence.animalId(),
                occurrence.type().databaseValue(),
                occurrence.severity().databaseValue(),
                occurrence.status().apiValue(),
                occurrence.description(),
                occurrence.source().databaseValue(),
                occurrence.recordedBy(),
                occurrence.createdAt()
        );
    }
}
