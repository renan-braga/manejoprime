package br.com.curralzap.backend.billing.application;

import br.com.curralzap.backend.animals.domain.AnimalRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
class PlanAnimalQuotaPolicy implements AnimalQuotaPolicy {

    private final BillingAccessPolicy billingAccessPolicy;
    private final AnimalRepository animalRepository;
    private final Map<String, Integer> planLimits;

    PlanAnimalQuotaPolicy(
            BillingAccessPolicy billingAccessPolicy,
            AnimalRepository animalRepository,
            @Value("${curralzap.plan-limits.free:50}") int freeLimit,
            @Value("${curralzap.plan-limits.starter:500}") int starterLimit,
            @Value("${curralzap.plan-limits.pro:2000}") int proLimit,
            @Value("${curralzap.plan-limits.business:-1}") int businessLimit,
            @Value("${curralzap.plan-limits.enterprise:-1}") int enterpriseLimit,
            @Value("${curralzap.plan-limits.internal:-1}") int internalLimit
    ) {
        this.billingAccessPolicy = billingAccessPolicy;
        this.animalRepository = animalRepository;
        this.planLimits = Map.of(
                "free", freeLimit,
                "starter", starterLimit,
                "pro", proLimit,
                "business", businessLimit,
                "enterprise", enterpriseLimit,
                "internal", internalLimit
        );
    }

    @Override
    public Integer maxAnimals(String plan) {
        int limit = planLimits.getOrDefault(plan, -1);
        return limit < 0 ? null : limit;
    }

    @Override
    public void requireQuotaAvailable(UUID farmId, int countToAdd) {
        BillingSnapshot snapshot = billingAccessPolicy.snapshotFor(farmId);
        Integer limit = maxAnimals(snapshot.plan());
        if (limit == null) {
            return;
        }
        long current = animalRepository.countByFarmId(farmId);
        if (current + countToAdd > limit) {
            throw new AnimalQuotaExceededException(snapshot.plan(), current, limit, countToAdd);
        }
    }
}
