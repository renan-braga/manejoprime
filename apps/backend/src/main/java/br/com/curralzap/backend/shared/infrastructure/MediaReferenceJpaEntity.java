package br.com.curralzap.backend.shared.infrastructure;

import br.com.curralzap.backend.shared.infrastructure.persistence.AbstractAuditableJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

@Entity
@Table(
        name = "media_references",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_media_references_owner_purpose",
                columnNames = {"owner_type", "owner_id", "purpose"}
        )
)
class MediaReferenceJpaEntity extends AbstractAuditableJpaEntity {

    @Column(name = "owner_type", nullable = false, length = 40)
    private String ownerType;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "purpose", nullable = false, length = 60)
    private String purpose;

    @Column(name = "external_url", length = 1000)
    private String externalUrl;

    @Column(name = "storage_path", length = 500)
    private String storagePath;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    protected MediaReferenceJpaEntity() {
    }

    String getOwnerType() {
        return ownerType;
    }

    void setOwnerType(String ownerType) {
        this.ownerType = ownerType;
    }

    UUID getOwnerId() {
        return ownerId;
    }

    void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    String getPurpose() {
        return purpose;
    }

    void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    String getExternalUrl() {
        return externalUrl;
    }

    void setExternalUrl(String externalUrl) {
        this.externalUrl = externalUrl;
    }

    String getStoragePath() {
        return storagePath;
    }

    void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    String getMimeType() {
        return mimeType;
    }

    void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    String getChecksumSha256() {
        return checksumSha256;
    }

    void setChecksumSha256(String checksumSha256) {
        this.checksumSha256 = checksumSha256;
    }
}
