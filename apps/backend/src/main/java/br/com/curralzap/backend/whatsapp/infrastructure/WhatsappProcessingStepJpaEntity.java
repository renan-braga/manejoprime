package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.shared.infrastructure.persistence.AbstractAuditableJpaEntity;
import br.com.curralzap.backend.whatsapp.domain.WhatsappProcessingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "whatsapp_processing_steps")
class WhatsappProcessingStepJpaEntity extends AbstractAuditableJpaEntity {

    @Column(name = "inbound_message_id", nullable = false)
    private UUID inboundMessageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private WhatsappProcessingStatus status;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    protected WhatsappProcessingStepJpaEntity() {
    }

    UUID getInboundMessageId() {
        return inboundMessageId;
    }

    void setInboundMessageId(UUID inboundMessageId) {
        this.inboundMessageId = inboundMessageId;
    }

    WhatsappProcessingStatus getStatus() {
        return status;
    }

    void setStatus(WhatsappProcessingStatus status) {
        this.status = status;
    }

    String getDetails() {
        return details;
    }

    void setDetails(String details) {
        this.details = details;
    }

    OffsetDateTime getOccurredAt() {
        return occurredAt;
    }

    void setOccurredAt(OffsetDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }
}
