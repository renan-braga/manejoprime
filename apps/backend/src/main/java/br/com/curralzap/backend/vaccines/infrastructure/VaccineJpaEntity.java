package br.com.curralzap.backend.vaccines.infrastructure;

import br.com.curralzap.backend.shared.infrastructure.persistence.AbstractFarmScopedAuditableJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "vaccines")
class VaccineJpaEntity extends AbstractFarmScopedAuditableJpaEntity {

    @Column(name = "animal_id")
    private UUID animalId;

    @Column(name = "lot_id")
    private UUID lotId;

    @Column(name = "vaccine_type", nullable = false, length = 200)
    private String vaccineType;

    @Column(name = "vaccine_batch", length = 100)
    private String vaccineBatch;

    @Column(name = "dose", nullable = false, length = 30)
    private String dose;

    @Column(name = "data_aplicacao", nullable = false)
    private LocalDate applicationDate;

    @Column(name = "data_proxima_dose")
    private LocalDate nextDoseDate;

    @Column(name = "volume_ml", precision = 6, scale = 2)
    private BigDecimal volumeMl;

    @Column(name = "origem", nullable = false, length = 30)
    private String source;

    @Column(name = "registrado_por", nullable = false)
    private UUID recordedBy;

    protected VaccineJpaEntity() {
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

    String getVaccineType() {
        return vaccineType;
    }

    void setVaccineType(String vaccineType) {
        this.vaccineType = vaccineType;
    }

    String getVaccineBatch() {
        return vaccineBatch;
    }

    void setVaccineBatch(String vaccineBatch) {
        this.vaccineBatch = vaccineBatch;
    }

    String getDose() {
        return dose;
    }

    void setDose(String dose) {
        this.dose = dose;
    }

    LocalDate getApplicationDate() {
        return applicationDate;
    }

    void setApplicationDate(LocalDate applicationDate) {
        this.applicationDate = applicationDate;
    }

    LocalDate getNextDoseDate() {
        return nextDoseDate;
    }

    void setNextDoseDate(LocalDate nextDoseDate) {
        this.nextDoseDate = nextDoseDate;
    }

    BigDecimal getVolumeMl() {
        return volumeMl;
    }

    void setVolumeMl(BigDecimal volumeMl) {
        this.volumeMl = volumeMl;
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
