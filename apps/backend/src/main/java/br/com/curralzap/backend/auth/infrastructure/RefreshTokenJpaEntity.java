package br.com.curralzap.backend.auth.infrastructure;

import br.com.curralzap.backend.shared.infrastructure.persistence.AbstractFarmScopedAuditableJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
class RefreshTokenJpaEntity extends AbstractFarmScopedAuditableJpaEntity {

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "consumed_at")
    private OffsetDateTime consumedAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "revocation_reason", length = 64)
    private String revocationReason;

    @Column(name = "replaced_by_token_id")
    private UUID replacedByTokenId;

    protected RefreshTokenJpaEntity() {
    }

    UUID getSessionId() {
        return sessionId;
    }

    void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    UUID getUserId() {
        return userId;
    }

    void setUserId(UUID userId) {
        this.userId = userId;
    }

    void setFarmIdValue(UUID farmId) {
        setFarmId(farmId);
    }

    OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    OffsetDateTime getConsumedAt() {
        return consumedAt;
    }

    void setConsumedAt(OffsetDateTime consumedAt) {
        this.consumedAt = consumedAt;
    }

    OffsetDateTime getRevokedAt() {
        return revokedAt;
    }

    void setRevokedAt(OffsetDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    String getRevocationReason() {
        return revocationReason;
    }

    void setRevocationReason(String revocationReason) {
        this.revocationReason = revocationReason;
    }

    UUID getReplacedByTokenId() {
        return replacedByTokenId;
    }

    void setReplacedByTokenId(UUID replacedByTokenId) {
        this.replacedByTokenId = replacedByTokenId;
    }
}
