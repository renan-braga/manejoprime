package br.com.curralzap.backend.medications.infrastructure;

import br.com.curralzap.backend.shared.infrastructure.persistence.AbstractFarmScopedAuditableJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "medications")
class MedicationJpaEntity extends AbstractFarmScopedAuditableJpaEntity {

    @Column(name = "animal_id", nullable = false)
    private UUID animalId;

    @Column(name = "occurrence_id")
    private UUID occurrenceId;

    @Column(name = "medication_name", nullable = false, length = 200)
    private String medicationName;

    @Column(name = "principio_ativo", length = 200)
    private String activeIngredient;

    @Column(name = "motivo")
    private String reason;

    @Column(name = "dosagem", nullable = false, length = 100)
    private String dosage;

    @Column(name = "data_aplicacao", nullable = false)
    private LocalDate applicationDate;

    @Column(name = "periodo_withdrawal", nullable = false)
    private int withdrawalPeriodDays;

    @Column(name = "release_date", nullable = false)
    private LocalDate releaseDate;

    @Column(name = "custo", precision = 10, scale = 2)
    private BigDecimal cost;

    @Column(name = "origem", nullable = false, length = 30)
    private String source;

    @Column(name = "registrado_por", nullable = false)
    private UUID recordedBy;

    protected MedicationJpaEntity() {
    }

    UUID getAnimalId() {
        return animalId;
    }

    void setAnimalId(UUID animalId) {
        this.animalId = animalId;
    }

    UUID getOccurrenceId() {
        return occurrenceId;
    }

    void setOccurrenceId(UUID occurrenceId) {
        this.occurrenceId = occurrenceId;
    }

    String getMedicationName() {
        return medicationName;
    }

    void setMedicationName(String medicationName) {
        this.medicationName = medicationName;
    }

    String getActiveIngredient() {
        return activeIngredient;
    }

    void setActiveIngredient(String activeIngredient) {
        this.activeIngredient = activeIngredient;
    }

    String getReason() {
        return reason;
    }

    void setReason(String reason) {
        this.reason = reason;
    }

    String getDosage() {
        return dosage;
    }

    void setDosage(String dosage) {
        this.dosage = dosage;
    }

    LocalDate getApplicationDate() {
        return applicationDate;
    }

    void setApplicationDate(LocalDate applicationDate) {
        this.applicationDate = applicationDate;
    }

    int getWithdrawalPeriodDays() {
        return withdrawalPeriodDays;
    }

    void setWithdrawalPeriodDays(int withdrawalPeriodDays) {
        this.withdrawalPeriodDays = withdrawalPeriodDays;
    }

    LocalDate getReleaseDate() {
        return releaseDate;
    }

    void setReleaseDate(LocalDate releaseDate) {
        this.releaseDate = releaseDate;
    }

    BigDecimal getCost() {
        return cost;
    }

    void setCost(BigDecimal cost) {
        this.cost = cost;
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
