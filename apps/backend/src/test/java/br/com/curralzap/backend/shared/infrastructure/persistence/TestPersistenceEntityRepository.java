package br.com.curralzap.backend.shared.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface TestPersistenceEntityRepository extends JpaRepository<TestPersistenceEntity, UUID> {
}
