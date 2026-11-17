package br.com.curralzap.backend.animals.infrastructure;

import br.com.curralzap.backend.shared.infrastructure.persistence.AbstractFarmScopedAuditableJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "animals")
class AnimalJpaEntity extends AbstractFarmScopedAuditableJpaEntity {

    @Column(name = "lot_id")
    private UUID lotId;

    @Column(name = "identificacao", nullable = false, length = 50)
    private String identifier;

    @Column(name = "nome", length = 100)
    private String name;

    @Column(name = "raca", length = 100)
    private String breed;

    @Column(name = "sexo", nullable = false, length = 20)
    private String sex;

    @Column(name = "data_nascimento")
    private LocalDate birthDate;

    @Column(name = "peso_nascimento", precision = 8, scale = 2)
    private BigDecimal birthWeightKg;

    @Column(name = "dam_id")
    private UUID damId;

    @Column(name = "sire_id")
    private UUID sireId;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "data_entrada")
    private LocalDate entryDate;

    protected AnimalJpaEntity() {
    }

    UUID getLotId() {
        return lotId;
    }

    void setLotId(UUID lotId) {
        this.lotId = lotId;
    }

    String getIdentifier() {
        return identifier;
    }

    void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    String getBreed() {
        return breed;
    }

    void setBreed(String breed) {
        this.breed = breed;
    }

    String getSex() {
        return sex;
    }

    void setSex(String sex) {
        this.sex = sex;
    }

    LocalDate getBirthDate() {
        return birthDate;
    }

    void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    BigDecimal getBirthWeightKg() {
        return birthWeightKg;
    }

    void setBirthWeightKg(BigDecimal birthWeightKg) {
        this.birthWeightKg = birthWeightKg;
    }

    UUID getDamId() {
        return damId;
    }

    void setDamId(UUID damId) {
        this.damId = damId;
    }

    UUID getSireId() {
        return sireId;
    }

    void setSireId(UUID sireId) {
        this.sireId = sireId;
    }

    String getStatus() {
        return status;
    }

    void setStatus(String status) {
        this.status = status;
    }

    LocalDate getEntryDate() {
        return entryDate;
    }

    void setEntryDate(LocalDate entryDate) {
        this.entryDate = entryDate;
    }

    void setFarmIdValue(UUID farmId) {
        setFarmId(farmId);
    }
}
