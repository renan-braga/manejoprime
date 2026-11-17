package br.com.curralzap.backend.auth.application;

import java.util.Optional;
import java.util.UUID;

public interface SessionMeViewRepository {

    Optional<SessionMeView> findByUserIdAndFarmId(UUID userId, UUID farmId);
}
