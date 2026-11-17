package br.com.curralzap.backend.shared.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;

import java.time.OffsetDateTime;

@MappedSuperclass
@EntityListeners(PersistenceAuditListener.class)
public abstract class AbstractAuditableJpaEntity extends AbstractUuidJpaEntity implements AuditableEntity {

    @Column(name = PersistenceConventions.CREATED_AT, nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = PersistenceConventions.UPDATED_AT, nullable = false)
    private OffsetDateTime updatedAt;

    @Override
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
