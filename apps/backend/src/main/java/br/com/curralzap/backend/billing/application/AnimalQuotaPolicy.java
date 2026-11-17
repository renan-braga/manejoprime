package br.com.curralzap.backend.billing.application;

import java.util.UUID;

public interface AnimalQuotaPolicy {

    /**
     * Returns the maximum number of animals allowed for the given plan,
     * or null if the plan has no limit.
     */
    Integer maxAnimals(String plan);

    void requireQuotaAvailable(UUID farmId, int countToAdd);
}
