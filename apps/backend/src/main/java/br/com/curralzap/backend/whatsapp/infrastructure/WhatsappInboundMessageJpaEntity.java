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
@Table(name = "whatsapp_inbound_messages")
class WhatsappInboundMessageJpaEntity extends AbstractAuditableJpaEntity {

    @Column(name = "provider_message_id", nullable = false, length = 255)
    private String providerMessageId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "phone_number", nullable = false, length = 32)
    private String phoneNumber;

    @Column(name = "farm_id")
    private UUID farmId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "correlation_id", length = 255)
    private String correlationId;

    @Column(name = "message_type", length = 100)
    private String messageType;

    @Column(name = "raw_payload", nullable = false, columnDefinition = "TEXT")
    private String rawPayload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private WhatsappProcessingStatus status;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt;

    protected WhatsappInboundMessageJpaEntity() {
    }

    String getProviderMessageId() {
        return providerMessageId;
    }

    void setProviderMessageId(String providerMessageId) {
        this.providerMessageId = providerMessageId;
    }

    String getEventType() {
        return eventType;
    }

    void setEventType(String eventType) {
        this.eventType = eventType;
    }

    String getPhoneNumber() {
        return phoneNumber;
    }

    void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    UUID getFarmIdValue() {
        return farmId;
    }

    void setFarmIdValue(UUID farmId) {
        this.farmId = farmId;
    }

    UUID getUserId() {
        return userId;
    }

    void setUserId(UUID userId) {
        this.userId = userId;
    }

    String getCorrelationId() {
        return correlationId;
    }

    void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    String getMessageType() {
        return messageType;
    }

    void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    String getRawPayload() {
        return rawPayload;
    }

    void setRawPayload(String rawPayload) {
        this.rawPayload = rawPayload;
    }

    WhatsappProcessingStatus getStatus() {
        return status;
    }

    void setStatus(WhatsappProcessingStatus status) {
        this.status = status;
    }

    String getFailureReason() {
        return failureReason;
    }

    void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    OffsetDateTime getReceivedAt() {
        return receivedAt;
    }

    void setReceivedAt(OffsetDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }
}
