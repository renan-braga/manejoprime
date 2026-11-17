package br.com.curralzap.backend.auth.infrastructure;

import br.com.curralzap.backend.shared.infrastructure.persistence.AbstractFarmScopedAuditableJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "auth_sessions")
class AuthSessionJpaEntity extends AbstractFarmScopedAuditableJpaEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "revocation_reason", length = 64)
    private String revocationReason;

    protected AuthSessionJpaEntity() {
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
}
