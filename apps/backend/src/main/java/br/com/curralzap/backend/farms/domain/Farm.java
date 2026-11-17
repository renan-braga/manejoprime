package br.com.curralzap.backend.farms.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Farm(UUID id, String name, UUID ownerUserId, OffsetDateTime createdAt, OffsetDateTime updatedAt) {

    public static Farm create(String name) {
        return new Farm(null, name, null, null, null);
    }
}
