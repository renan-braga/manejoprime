package br.com.curralzap.backend.offers.infrastructure;

import br.com.curralzap.backend.shared.infrastructure.persistence.AbstractFarmScopedAuditableJpaEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "offers")
class OfferJpaEntity extends AbstractFarmScopedAuditableJpaEntity {

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "asking_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal askingPrice;

    @Column(name = "buyer_contact", length = 200)
    private String buyerContact;

    @Column(name = "expires_at")
    private LocalDate expiresAt;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "offer_animals", joinColumns = @JoinColumn(name = "offer_id"))
    @Column(name = "animal_id")
    private List<UUID> animalIds = new ArrayList<>();

    protected OfferJpaEntity() {
    }

    String getTitle() { return title; }
    void setTitle(String title) { this.title = title; }

    String getNotes() { return notes; }
    void setNotes(String notes) { this.notes = notes; }

    BigDecimal getAskingPrice() { return askingPrice; }
    void setAskingPrice(BigDecimal askingPrice) { this.askingPrice = askingPrice; }

    String getBuyerContact() { return buyerContact; }
    void setBuyerContact(String buyerContact) { this.buyerContact = buyerContact; }

    LocalDate getExpiresAt() { return expiresAt; }
    void setExpiresAt(LocalDate expiresAt) { this.expiresAt = expiresAt; }

    String getStatus() { return status; }
    void setStatus(String status) { this.status = status; }

    List<UUID> getAnimalIds() { return animalIds; }
    void setAnimalIds(List<UUID> animalIds) {
        this.animalIds.clear();
        this.animalIds.addAll(animalIds);
    }

    void setFarmIdValue(UUID farmId) {
        setFarmId(farmId);
    }
}
