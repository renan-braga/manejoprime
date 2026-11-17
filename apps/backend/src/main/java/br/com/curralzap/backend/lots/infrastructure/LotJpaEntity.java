package br.com.curralzap.backend.lots.infrastructure;

import br.com.curralzap.backend.shared.infrastructure.persistence.AbstractFarmScopedAuditableJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "lots")
class LotJpaEntity extends AbstractFarmScopedAuditableJpaEntity {

    @Column(name = "nome", nullable = false, length = 100)
    private String name;

    @Column(name = "finalidade", nullable = false, length = 30)
    private String purpose;

    @Column(name = "pasto_area", length = 100)
    private String pastureArea;

    protected LotJpaEntity() {
    }

    String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    String getPurpose() {
        return purpose;
    }

    void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    String getPastureArea() {
        return pastureArea;
    }

    void setPastureArea(String pastureArea) {
        this.pastureArea = pastureArea;
    }

    void setFarmIdValue(UUID farmId) {
        setFarmId(farmId);
    }
}
