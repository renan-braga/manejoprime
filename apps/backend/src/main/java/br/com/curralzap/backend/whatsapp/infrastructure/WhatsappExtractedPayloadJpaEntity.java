package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.shared.infrastructure.persistence.AbstractAuditableJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "whatsapp_extracted_payloads")
class WhatsappExtractedPayloadJpaEntity extends AbstractAuditableJpaEntity {

    @Column(name = "inbound_message_id", nullable = false, unique = true)
    private UUID inboundMessageId;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "provider_model", nullable = false, length = 100)
    private String providerModel;

    @Column(name = "intent", length = 100)
    private String intent;

    @Column(name = "structured_payload", nullable = false, columnDefinition = "TEXT")
    private String structuredPayload;

    @Column(name = "extracted_at", nullable = false)
    private OffsetDateTime extractedAt;

    protected WhatsappExtractedPayloadJpaEntity() {
    }

    UUID getInboundMessageId() {
        return inboundMessageId;
    }

    void setInboundMessageId(UUID inboundMessageId) {
        this.inboundMessageId = inboundMessageId;
    }

    String getProvider() {
        return provider;
    }

    void setProvider(String provider) {
        this.provider = provider;
    }

    String getProviderModel() {
        return providerModel;
    }

    void setProviderModel(String providerModel) {
        this.providerModel = providerModel;
    }

    String getIntent() {
        return intent;
    }

    void setIntent(String intent) {
        this.intent = intent;
    }

    String getStructuredPayload() {
        return structuredPayload;
    }

    void setStructuredPayload(String structuredPayload) {
        this.structuredPayload = structuredPayload;
    }

    OffsetDateTime getExtractedAt() {
        return extractedAt;
    }

    void setExtractedAt(OffsetDateTime extractedAt) {
        this.extractedAt = extractedAt;
    }
}
