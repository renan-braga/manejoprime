package br.com.curralzap.backend.shared.infrastructure.persistence;

import java.time.OffsetDateTime;

public interface SoftDeletable {

    OffsetDateTime getDeletedAt();

    default boolean isDeleted() {
        return getDeletedAt() != null;
    }
}
