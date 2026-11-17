package br.com.curralzap.backend.farms.domain;

import java.util.UUID;

public interface FarmRepository {
    Farm save(Farm farm);
    void setOwner(UUID farmId, UUID ownerUserId);
    int countOwnedByUserId(UUID ownerUserId);
    java.util.Optional<UUID> findFirstOwnedByUserId(UUID ownerUserId);
}
