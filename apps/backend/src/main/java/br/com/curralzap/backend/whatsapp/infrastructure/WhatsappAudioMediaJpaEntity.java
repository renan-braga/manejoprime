package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.shared.infrastructure.persistence.AbstractAuditableJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "whatsapp_audio_media")
class WhatsappAudioMediaJpaEntity extends AbstractAuditableJpaEntity {

    @Column(name = "inbound_message_id", nullable = false, unique = true)
    private UUID inboundMessageId;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @Column(name = "downloaded_at", nullable = false)
    private OffsetDateTime downloadedAt;

    @Column(name = "transcoded_mime_type", length = 100)
    private String transcodedMimeType;

    @Column(name = "transcoded_storage_path", length = 500)
    private String transcodedStoragePath;

    @Column(name = "transcoded_at")
    private OffsetDateTime transcodedAt;

    protected WhatsappAudioMediaJpaEntity() {
    }

    UUID getInboundMessageId() {
        return inboundMessageId;
    }

    void setInboundMessageId(UUID inboundMessageId) {
        this.inboundMessageId = inboundMessageId;
    }

    String getSourceUrl() {
        return sourceUrl;
    }

    void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    String getMimeType() {
        return mimeType;
    }

    void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    String getStoragePath() {
        return storagePath;
    }

    void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    OffsetDateTime getDownloadedAt() {
        return downloadedAt;
    }

    void setDownloadedAt(OffsetDateTime downloadedAt) {
        this.downloadedAt = downloadedAt;
    }

    String getTranscodedMimeType() {
        return transcodedMimeType;
    }

    void setTranscodedMimeType(String transcodedMimeType) {
        this.transcodedMimeType = transcodedMimeType;
    }

    String getTranscodedStoragePath() {
        return transcodedStoragePath;
    }

    void setTranscodedStoragePath(String transcodedStoragePath) {
        this.transcodedStoragePath = transcodedStoragePath;
    }

    OffsetDateTime getTranscodedAt() {
        return transcodedAt;
    }

    void setTranscodedAt(OffsetDateTime transcodedAt) {
        this.transcodedAt = transcodedAt;
    }
}
