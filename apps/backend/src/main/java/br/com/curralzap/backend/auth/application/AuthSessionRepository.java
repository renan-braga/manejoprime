package br.com.curralzap.backend.auth.application;

import br.com.curralzap.backend.auth.domain.AuthSession;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthSessionRepository {

    AuthSession save(AuthSession session);

    List<AuthSession> findActiveByUserId(UUID userId);

    Optional<AuthSession> findById(UUID sessionId);
}
