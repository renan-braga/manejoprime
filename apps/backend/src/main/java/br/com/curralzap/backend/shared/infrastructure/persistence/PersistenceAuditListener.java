package br.com.curralzap.backend.shared.infrastructure.persistence;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.OffsetDateTime;

public class PersistenceAuditListener {

    @PrePersist
    void onPrePersist(AbstractAuditableJpaEntity entity) {
        OffsetDateTime now = PersistenceConventions.nowUtc();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
    }

    @PreUpdate
    void onPreUpdate(AbstractAuditableJpaEntity entity) {
        entity.setUpdatedAt(PersistenceConventions.nowUtc());
    }
}
