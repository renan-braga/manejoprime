package br.com.curralzap.backend.farms.application;

import br.com.curralzap.backend.animals.domain.AnimalRepository;
import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.auth.application.AuthenticationRequiredException;
import br.com.curralzap.backend.billing.application.AnimalQuotaPolicy;
import br.com.curralzap.backend.billing.application.BillingAccessPolicy;
import br.com.curralzap.backend.billing.application.BillingSnapshot;
import br.com.curralzap.backend.lots.domain.LotRepository;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserAccountRepository;
import br.com.curralzap.backend.vaccines.domain.VaccineRepository;
import org.springframework.stereotype.Service;

@Service
public class OnboardingStatusService {

    private final UserAccountRepository userAccountRepository;
    private final AnimalRepository animalRepository;
    private final LotRepository lotRepository;
    private final VaccineRepository vaccineRepository;
    private final BillingAccessPolicy billingAccessPolicy;
    private final AnimalQuotaPolicy animalQuotaPolicy;

    public OnboardingStatusService(
            UserAccountRepository userAccountRepository,
            AnimalRepository animalRepository,
            LotRepository lotRepository,
            VaccineRepository vaccineRepository,
            BillingAccessPolicy billingAccessPolicy,
            AnimalQuotaPolicy animalQuotaPolicy
    ) {
        this.userAccountRepository = userAccountRepository;
        this.animalRepository = animalRepository;
        this.lotRepository = lotRepository;
        this.vaccineRepository = vaccineRepository;
        this.billingAccessPolicy = billingAccessPolicy;
        this.animalQuotaPolicy = animalQuotaPolicy;
    }

    public OnboardingStatusView status(AuthTokenClaims claims) {
        UserAccount user = userAccountRepository.findById(claims.userId())
                .orElseThrow(AuthenticationRequiredException::new);

        boolean adminHasPhone = user.phone() != null && !user.phone().isBlank();
        boolean hasLots = lotRepository.existsByFarmId(claims.farmId());
        boolean hasAnimals = animalRepository.existsByFarmId(claims.farmId());
        boolean hasVaccines = vaccineRepository.existsByFarmId(claims.farmId());

        long animalCount = animalRepository.countByFarmId(claims.farmId());
        BillingSnapshot snapshot = billingAccessPolicy.snapshotFor(claims.farmId());
        Integer planLimit = animalQuotaPolicy.maxAnimals(snapshot.plan());

        return new OnboardingStatusView(adminHasPhone, hasLots, hasAnimals, hasVaccines, animalCount, planLimit);
    }
}
