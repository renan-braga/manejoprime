package br.com.curralzap.backend.auth.infrastructure;

import br.com.curralzap.backend.auth.application.AuthSessionRepository;
import br.com.curralzap.backend.auth.domain.AuthSession;
import br.com.curralzap.backend.auth.domain.SessionRevocationReason;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class JpaAuthSessionRepository implements AuthSessionRepository {

    private final AuthSessionJpaSpringRepository repository;

    JpaAuthSessionRepository(AuthSessionJpaSpringRepository repository) {
        this.repository = repository;
    }

    @Override
    public AuthSession save(AuthSession session) {
        AuthSessionJpaEntity entity = session.id() == null
                ? new AuthSessionJpaEntity()
                : repository.findById(session.id()).orElseGet(AuthSessionJpaEntity::new);
        entity.setFarmIdValue(session.farmId());
        entity.setUserId(session.userId());
        entity.setRevokedAt(session.revokedAt());
        entity.setRevocationReason(session.revocationReason() == null ? null : session.revocationReason().databaseValue());
        return toDomain(repository.save(entity));
    }

    @Override
    public List<AuthSession> findActiveByUserId(UUID userId) {
        return repository.findAllByUserIdAndRevokedAtIsNull(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<AuthSession> findById(UUID sessionId) {
        return repository.findById(sessionId).map(this::toDomain);
    }

    private AuthSession toDomain(AuthSessionJpaEntity entity) {
        return new AuthSession(
                entity.getId(),
                entity.getUserId(),
                entity.getFarmId(),
                entity.getRevokedAt(),
                entity.getRevocationReason() == null ? null : SessionRevocationReason.fromDatabaseValue(entity.getRevocationReason()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
