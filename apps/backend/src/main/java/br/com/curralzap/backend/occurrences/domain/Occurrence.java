package br.com.curralzap.backend.occurrences.domain;

import br.com.curralzap.backend.occurrences.application.InvalidOccurrenceStateTransitionException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

public record Occurrence(
        UUID id,
        UUID farmId,
        UUID animalId,
        OccurrenceType type,
        OccurrenceSeverity severity,
        String description,
        String photoUrl,
        OccurrenceSource source,
        UUID recordedBy,
        OccurrenceStatus status,
        UUID resolvedBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static Occurrence register(
            UUID farmId,
            UUID animalId,
            UUID recordedBy,
            OccurrenceType type,
            OccurrenceSeverity severity,
            String description
    ) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return new Occurrence(
                UUID.randomUUID(),
                farmId,
                animalId,
                type,
                severity,
                description,
                null,
                OccurrenceSource.APP,
                recordedBy,
                OccurrenceStatus.OPEN,
                null,
                now,
                now
        );
    }

    public Occurrence resolve(UUID resolvedBy, OffsetDateTime resolvedAt) {
        if (status == OccurrenceStatus.RESOLVED) {
            throw new InvalidOccurrenceStateTransitionException();
        }
        return new Occurrence(
                id,
                farmId,
                animalId,
                type,
                severity,
                description,
                photoUrl,
                source,
                recordedBy,
                OccurrenceStatus.RESOLVED,
                resolvedBy,
                createdAt,
                resolvedAt
        );
    }

    public Occurrence reopen(OffsetDateTime reopenedAt) {
        if (status != OccurrenceStatus.RESOLVED) {
            throw new InvalidOccurrenceStateTransitionException();
        }
        return new Occurrence(
                id,
                farmId,
                animalId,
                type,
                severity,
                description,
                photoUrl,
                source,
                recordedBy,
                OccurrenceStatus.OPEN,
                null,
                createdAt,
                reopenedAt
        );
    }
}
