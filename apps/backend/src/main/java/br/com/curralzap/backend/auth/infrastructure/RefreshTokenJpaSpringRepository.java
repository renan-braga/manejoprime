package br.com.curralzap.backend.auth.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface RefreshTokenJpaSpringRepository extends JpaRepository<RefreshTokenJpaEntity, UUID> {

    List<RefreshTokenJpaEntity> findAllBySessionIdAndRevokedAtIsNull(UUID sessionId);
}
