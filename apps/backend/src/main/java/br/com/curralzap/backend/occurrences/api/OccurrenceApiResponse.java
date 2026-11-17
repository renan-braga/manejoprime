package br.com.curralzap.backend.occurrences.api;

import br.com.curralzap.backend.occurrences.application.OccurrenceView;

import java.time.OffsetDateTime;
import java.util.UUID;

record OccurrenceApiResponse(
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
    static OccurrenceApiResponse from(OccurrenceView view) {
        return new OccurrenceApiResponse(
                view.id(),
                view.animalId(),
                view.type(),
                view.severity(),
                view.status(),
                view.description(),
                view.source(),
                view.recordedBy(),
                view.createdAt()
        );
    }
}
