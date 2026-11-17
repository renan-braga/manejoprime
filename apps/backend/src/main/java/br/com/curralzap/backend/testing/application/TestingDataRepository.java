package br.com.curralzap.backend.testing.application;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface TestingDataRepository {

    void insertActiveSubscription(UUID farmId, OffsetDateTime now);

    void deleteAllForFarm(UUID farmId);
}
