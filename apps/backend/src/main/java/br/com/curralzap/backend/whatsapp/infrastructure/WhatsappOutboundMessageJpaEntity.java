package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.shared.infrastructure.persistence.AbstractAuditableJpaEntity;
import br.com.curralzap.backend.whatsapp.domain.WhatsappOutboundStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "whatsapp_outbound_messages")
class WhatsappOutboundMessageJpaEntity extends AbstractAuditableJpaEntity {

    @Column(name = "inbound_message_id", nullable = false)
    private UUID inboundMessageId;

    @Column(name = "farm_id")
    private UUID farmId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "correlation_id", nullable = false, length = 255)
    private String correlationId;

    @Column(name = "phone_number", nullable = false, length = 32)
    private String phoneNumber;

    @Column(name = "message_type", nullable = false, length = 100)
    private String messageType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "provider_message_id", length = 255)
    private String providerMessageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private WhatsappOutboundStatus status;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "queued_at", nullable = false)
    private OffsetDateTime queuedAt;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    protected WhatsappOutboundMessageJpaEntity() {
    }

    UUID getInboundMessageId() {
        return inboundMessageId;
    }

    void setInboundMessageId(UUID inboundMessageId) {
        this.inboundMessageId = inboundMessageId;
    }

    UUID getFarmId() {
        return farmId;
    }

    void setFarmId(UUID farmId) {
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

    String getPhoneNumber() {
        return phoneNumber;
    }

    void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    String getMessageType() {
        return messageType;
    }

    void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    String getPayload() {
        return payload;
    }

    void setPayload(String payload) {
        this.payload = payload;
    }

    String getProviderMessageId() {
        return providerMessageId;
    }

    void setProviderMessageId(String providerMessageId) {
        this.providerMessageId = providerMessageId;
    }

    WhatsappOutboundStatus getStatus() {
        return status;
    }

    void setStatus(WhatsappOutboundStatus status) {
        this.status = status;
    }

    String getFailureReason() {
        return failureReason;
    }

    void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    OffsetDateTime getQueuedAt() {
        return queuedAt;
    }

    void setQueuedAt(OffsetDateTime queuedAt) {
        this.queuedAt = queuedAt;
    }

    OffsetDateTime getSentAt() {
        return sentAt;
    }

    void setSentAt(OffsetDateTime sentAt) {
        this.sentAt = sentAt;
    }
}
