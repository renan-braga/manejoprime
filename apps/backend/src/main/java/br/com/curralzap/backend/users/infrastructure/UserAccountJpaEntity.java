package br.com.curralzap.backend.users.infrastructure;

import br.com.curralzap.backend.shared.infrastructure.persistence.AbstractFarmScopedAuditableJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "users")
class UserAccountJpaEntity extends AbstractFarmScopedAuditableJpaEntity {

    @Column(name = "telefone", nullable = true, length = 20)
    private String phone;

    @Column(name = "email", nullable = true)
    private String email;

    @Column(name = "nome", nullable = false)
    private String name;

    @Column(name = "role", nullable = false, length = 30)
    private String role;

    @Column(name = "ativo", nullable = false)
    private boolean active;

    protected UserAccountJpaEntity() {
    }

    String getPhone() {
        return phone;
    }

    String getEmail() {
        return email;
    }

    String getName() {
        return name;
    }

    String getRole() {
        return role;
    }

    boolean isActive() {
        return active;
    }

    void setFarmIdValue(UUID farmId) {
        setFarmId(farmId);
    }

    void setPhone(String phone) {
        this.phone = phone;
    }

    void setEmail(String email) {
        this.email = email;
    }

    void setName(String name) {
        this.name = name;
    }

    void setRole(String role) {
        this.role = role;
    }

    void setActive(boolean active) {
        this.active = active;
    }
}
