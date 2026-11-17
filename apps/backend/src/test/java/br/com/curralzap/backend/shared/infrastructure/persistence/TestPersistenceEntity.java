package br.com.curralzap.backend.shared.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "test_persistence_entity")
class TestPersistenceEntity extends AbstractSoftDeletableFarmScopedAuditableJpaEntity {

    @Column(name = "display_name", nullable = false)
    private String displayName;

    protected TestPersistenceEntity() {
    }

    TestPersistenceEntity(UUID farmId, String displayName) {
        setFarmId(farmId);
        this.displayName = displayName;
    }

    String getDisplayName() {
        return displayName;
    }

    void rename(String displayName) {
        this.displayName = displayName;
    }

    void markDeletedForTest(OffsetDateTime deletedAt) {
        markDeleted(deletedAt);
    }
}
