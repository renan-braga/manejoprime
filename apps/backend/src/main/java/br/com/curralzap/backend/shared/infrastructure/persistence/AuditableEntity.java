package br.com.curralzap.backend.shared.infrastructure.persistence;

import java.time.OffsetDateTime;

public interface AuditableEntity {

    OffsetDateTime getCreatedAt();

    OffsetDateTime getUpdatedAt();
}
