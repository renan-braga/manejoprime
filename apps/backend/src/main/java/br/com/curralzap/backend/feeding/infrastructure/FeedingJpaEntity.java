package br.com.curralzap.backend.feeding.infrastructure;

import br.com.curralzap.backend.shared.infrastructure.persistence.AbstractFarmScopedAuditableJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "alimentacoes")
class FeedingJpaEntity extends AbstractFarmScopedAuditableJpaEntity {

    @Column(name = "animal_id")
    private UUID animalId;

    @Column(name = "lot_id")
    private UUID lotId;

    @Column(name = "tipo_alimento", nullable = false, length = 30)
    private String feedType;

    @Column(name = "descricao")
    private String description;

    @Column(name = "quantidade_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantityKg;

    @Column(name = "custo_por_kg", precision = 10, scale = 2)
    private BigDecimal costPerKg;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate startDate;

    @Column(name = "data_fim")
    private LocalDate endDate;

    @Column(name = "origem", nullable = false, length = 30)
    private String source;

    @Column(name = "registrado_por", nullable = false)
    private UUID recordedBy;

    protected FeedingJpaEntity() {
    }

    UUID getAnimalId() {
        return animalId;
    }

    void setAnimalId(UUID animalId) {
        this.animalId = animalId;
    }

    UUID getLotId() {
        return lotId;
    }

    void setLotId(UUID lotId) {
        this.lotId = lotId;
    }

    String getFeedType() {
        return feedType;
    }

    void setFeedType(String feedType) {
        this.feedType = feedType;
    }

    String getDescription() {
        return description;
    }

    void setDescription(String description) {
        this.description = description;
    }

    BigDecimal getQuantityKg() {
        return quantityKg;
    }

    void setQuantityKg(BigDecimal quantityKg) {
        this.quantityKg = quantityKg;
    }

    BigDecimal getCostPerKg() {
        return costPerKg;
    }

    void setCostPerKg(BigDecimal costPerKg) {
        this.costPerKg = costPerKg;
    }

    LocalDate getStartDate() {
        return startDate;
    }

    void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    LocalDate getEndDate() {
        return endDate;
    }

    void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    String getSource() {
        return source;
    }

    void setSource(String source) {
        this.source = source;
    }

    UUID getRecordedBy() {
        return recordedBy;
    }

    void setRecordedBy(UUID recordedBy) {
        this.recordedBy = recordedBy;
    }

    void setFarmIdValue(UUID farmId) {
        setFarmId(farmId);
    }
}
