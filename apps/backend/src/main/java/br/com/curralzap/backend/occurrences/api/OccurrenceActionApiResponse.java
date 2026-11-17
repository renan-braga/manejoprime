package br.com.curralzap.backend.occurrences.api;

import br.com.curralzap.backend.occurrences.application.OccurrenceActionResult;

import java.time.OffsetDateTime;
import java.util.UUID;

record OccurrenceActionApiResponse(
        UUID id,
        String status,
        UUID resolvedBy,
        OffsetDateTime resolvedAt
) {
    static OccurrenceActionApiResponse from(OccurrenceActionResult result) {
        return new OccurrenceActionApiResponse(
                result.id(),
                result.status(),
                result.resolvedBy(),
                result.resolvedAt()
        );
    }
}
