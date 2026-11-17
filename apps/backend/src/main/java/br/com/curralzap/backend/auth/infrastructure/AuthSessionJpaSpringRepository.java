package br.com.curralzap.backend.auth.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface AuthSessionJpaSpringRepository extends JpaRepository<AuthSessionJpaEntity, UUID> {

    List<AuthSessionJpaEntity> findAllByUserIdAndRevokedAtIsNull(UUID userId);
}
