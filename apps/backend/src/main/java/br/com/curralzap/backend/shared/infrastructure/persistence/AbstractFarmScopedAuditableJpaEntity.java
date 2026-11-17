package br.com.curralzap.backend.shared.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import java.util.UUID;

@MappedSuperclass
public abstract class AbstractFarmScopedAuditableJpaEntity extends AbstractAuditableJpaEntity implements FarmScoped {

    @Column(name = PersistenceConventions.FARM_ID, nullable = false)
    private UUID farmId;

    @Override
    public UUID getFarmId() {
        return farmId;
    }

    protected void setFarmId(UUID farmId) {
        this.farmId = farmId;
    }
}
