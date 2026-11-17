package br.com.curralzap.backend.users.infrastructure;

import br.com.curralzap.backend.shared.infrastructure.persistence.AbstractFarmScopedAuditableJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "invitations")
class InvitationJpaEntity extends AbstractFarmScopedAuditableJpaEntity {

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "role", nullable = false, length = 30)
    private String role;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "invited_by")
    private UUID invitedBy;

    protected InvitationJpaEntity() {
    }

    String getEmail() { return email; }
    String getPhone() { return phone; }
    String getRole() { return role; }
    String getStatus() { return status; }
    UUID getInvitedBy() { return invitedBy; }

    void setFarmIdValue(UUID farmId) { setFarmId(farmId); }
    void setEmail(String email) { this.email = email; }
    void setPhone(String phone) { this.phone = phone; }
    void setRole(String role) { this.role = role; }
    void setStatus(String status) { this.status = status; }
    void setInvitedBy(UUID invitedBy) { this.invitedBy = invitedBy; }
}
