package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.shared.infrastructure.persistence.AbstractAuditableJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "whatsapp_transcripts")
class WhatsappTranscriptJpaEntity extends AbstractAuditableJpaEntity {

    @Column(name = "inbound_message_id", nullable = false, unique = true)
    private UUID inboundMessageId;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "provider_model", nullable = false, length = 100)
    private String providerModel;

    @Column(name = "transcript", nullable = false, columnDefinition = "TEXT")
    private String transcript;

    @Column(name = "transcribed_at", nullable = false)
    private OffsetDateTime transcribedAt;

    protected WhatsappTranscriptJpaEntity() {
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

    String getTranscript() {
        return transcript;
    }

    void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    OffsetDateTime getTranscribedAt() {
        return transcribedAt;
    }

    void setTranscribedAt(OffsetDateTime transcribedAt) {
        this.transcribedAt = transcribedAt;
    }
}
