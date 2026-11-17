package br.com.curralzap.backend.farms.application;

import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.billing.application.BillingAccessPolicy;
import br.com.curralzap.backend.billing.application.BillingSnapshot;
import br.com.curralzap.backend.farms.domain.Farm;
import br.com.curralzap.backend.farms.domain.FarmRepository;
import br.com.curralzap.backend.farms.domain.UserFarmMembershipRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MyFarmsService {

    private static final Map<String, Integer> PLAN_FARM_LIMITS = Map.of(
            "free", 1,
            "starter", 1,
            "pro", 2,
            "business", 5,
            "trialing", 2
    );

    private final FarmRepository farmRepository;
    private final UserFarmMembershipRepository membershipRepository;
    private final BillingAccessPolicy billingAccessPolicy;

    public MyFarmsService(
            FarmRepository farmRepository,
            UserFarmMembershipRepository membershipRepository,
            BillingAccessPolicy billingAccessPolicy
    ) {
        this.farmRepository = farmRepository;
        this.membershipRepository = membershipRepository;
        this.billingAccessPolicy = billingAccessPolicy;
    }

    public List<FarmSummaryView> listFarms(AuthTokenClaims claims) {
        return membershipRepository.findWithFarmNameByUserId(claims.userId()).stream()
                .map(m -> new FarmSummaryView(
                        m.farmId(),
                        m.farmName(),
                        m.role(),
                        m.farmId().equals(claims.farmId())
                ))
                .toList();
    }

    public int maxFarmsForPlan(AuthTokenClaims claims) {
        // Resolve billing from a farm the user OWNS, not the current session farm.
        // Without this, a user on a shared farm with a higher plan could bypass their own limit.
        UUID billingFarmId = farmRepository.findFirstOwnedByUserId(claims.userId())
                .orElse(claims.farmId());
        BillingSnapshot snapshot = billingAccessPolicy.snapshotFor(billingFarmId);
        // Suspended/canceled accounts revert to free-tier farm limit.
        String effectivePlan = snapshot.canWrite() ? snapshot.plan() : "free";
        return PLAN_FARM_LIMITS.getOrDefault(effectivePlan, -1);
    }

    @Transactional
    public FarmSummaryView addFarm(AuthTokenClaims claims, String farmName) {
        if (farmName == null || farmName.isBlank() || farmName.length() > 255) {
            throw new InvalidFarmRegistrationException(
                    Map.of("farmName", "Nome da fazenda inválido.")
            );
        }

        // Count only farms the user OWNS, not farms they joined as member.
        int currentCount = farmRepository.countOwnedByUserId(claims.userId());
        int limit = maxFarmsForPlan(claims);
        if (limit >= 0 && currentCount >= limit) {
            throw new FarmQuotaExceededException(limit);
        }

        Farm farm = farmRepository.save(Farm.create(farmName));
        farmRepository.setOwner(farm.id(), claims.userId());
        membershipRepository.save(claims.userId(), farm.id(), "admin");

        return new FarmSummaryView(farm.id(), farm.name(), "admin", false);
    }
}
