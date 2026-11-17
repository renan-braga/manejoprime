package br.com.curralzap.backend.auth.infrastructure;

import br.com.curralzap.backend.auth.application.RefreshTokenRepository;
import br.com.curralzap.backend.auth.domain.RefreshToken;
import br.com.curralzap.backend.auth.domain.SessionRevocationReason;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class JpaRefreshTokenRepository implements RefreshTokenRepository {

    private final RefreshTokenJpaSpringRepository repository;

    JpaRefreshTokenRepository(RefreshTokenJpaSpringRepository repository) {
        this.repository = repository;
    }

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        RefreshTokenJpaEntity entity = repository.findById(refreshToken.id()).orElseGet(RefreshTokenJpaEntity::new);
        entity.setSessionId(refreshToken.sessionId());
        entity.setFarmIdValue(refreshToken.farmId());
        entity.setUserId(refreshToken.userId());
        entity.setExpiresAt(refreshToken.expiresAt());
        entity.setConsumedAt(refreshToken.consumedAt());
        entity.setRevokedAt(refreshToken.revokedAt());
        entity.setRevocationReason(refreshToken.revocationReason() == null ? null : refreshToken.revocationReason().databaseValue());
        entity.setReplacedByTokenId(refreshToken.replacedByTokenId());
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<RefreshToken> findById(UUID tokenId) {
        return repository.findById(tokenId).map(this::toDomain);
    }

    @Override
    public List<RefreshToken> findAllBySessionIdAndNotRevoked(UUID sessionId) {
        return repository.findAllBySessionIdAndRevokedAtIsNull(sessionId).stream()
                .map(this::toDomain)
                .toList();
    }

    private RefreshToken toDomain(RefreshTokenJpaEntity entity) {
        return new RefreshToken(
                entity.getId(),
                entity.getSessionId(),
                entity.getUserId(),
                entity.getFarmId(),
                entity.getExpiresAt(),
                entity.getConsumedAt(),
                entity.getRevokedAt(),
                entity.getRevocationReason() == null ? null : SessionRevocationReason.fromDatabaseValue(entity.getRevocationReason()),
                entity.getReplacedByTokenId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
