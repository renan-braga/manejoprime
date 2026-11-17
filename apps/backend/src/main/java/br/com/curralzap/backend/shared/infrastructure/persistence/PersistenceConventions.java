package br.com.curralzap.backend.shared.infrastructure.persistence;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class PersistenceConventions {

    private PersistenceConventions() {
    }

    public static final String ID = "id";
    public static final String FARM_ID = "farm_id";
    public static final String CREATED_AT = "created_at";
    public static final String UPDATED_AT = "updated_at";
    public static final String DELETED_AT = "deleted_at";

    public static OffsetDateTime nowUtc() {
        return OffsetDateTime.now().withOffsetSameInstant(java.time.ZoneOffset.UTC);
    }

    public static UUID newUuid() {
        return UUID.randomUUID();
    }
}
