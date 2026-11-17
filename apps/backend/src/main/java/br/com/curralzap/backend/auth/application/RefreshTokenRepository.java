package br.com.curralzap.backend.auth.application;

import br.com.curralzap.backend.auth.domain.RefreshToken;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

    RefreshToken save(RefreshToken refreshToken);

    Optional<RefreshToken> findById(UUID tokenId);

    List<RefreshToken> findAllBySessionIdAndNotRevoked(UUID sessionId);
}
