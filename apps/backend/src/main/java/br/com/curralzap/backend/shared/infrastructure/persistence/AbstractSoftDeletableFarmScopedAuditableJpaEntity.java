package br.com.curralzap.backend.shared.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import java.time.OffsetDateTime;

@MappedSuperclass
public abstract class AbstractSoftDeletableFarmScopedAuditableJpaEntity extends AbstractFarmScopedAuditableJpaEntity implements SoftDeletable {

    @Column(name = PersistenceConventions.DELETED_AT)
    private OffsetDateTime deletedAt;

    @Override
    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    protected void markDeleted(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
