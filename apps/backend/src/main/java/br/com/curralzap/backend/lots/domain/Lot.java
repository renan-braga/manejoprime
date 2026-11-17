package br.com.curralzap.backend.lots.domain;

import br.com.curralzap.backend.lots.application.LotUpsertInput;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Lot(
        UUID id,
        UUID farmId,
        String name,
        LotPurpose purpose,
        String pastureArea,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static Lot create(UUID farmId, LotUpsertInput input) {
        return new Lot(
                null,
                farmId,
                input.name(),
                input.purpose(),
                input.pastureArea(),
                null,
                null
        );
    }

    public Lot update(LotUpsertInput input) {
        return new Lot(
                id,
                farmId,
                input.name(),
                input.purpose(),
                input.pastureArea(),
                createdAt,
                updatedAt
        );
    }
}
