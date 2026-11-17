package br.com.curralzap.backend.occurrences.application;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OccurrenceActionResult(
        UUID id,
        String status,
        UUID resolvedBy,
        OffsetDateTime resolvedAt
) {
}
