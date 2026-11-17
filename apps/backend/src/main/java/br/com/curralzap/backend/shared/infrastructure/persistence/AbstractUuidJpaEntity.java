package br.com.curralzap.backend.shared.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import java.util.UUID;

@MappedSuperclass
public abstract class AbstractUuidJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = PersistenceConventions.ID, nullable = false, updatable = false)
    private UUID id;

    public UUID getId() {
        return id;
    }
}
