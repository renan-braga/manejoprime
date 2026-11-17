package br.com.curralzap.backend.billing.application;

import br.com.curralzap.backend.billing.domain.BillingSubscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class BillingAccessGuard implements BillingAccessPolicy {

    private final BillingSubscriptionRepository subscriptionRepository;
    private final Clock clock;

    @Autowired
    public BillingAccessGuard(BillingSubscriptionRepository subscriptionRepository) {
        this(subscriptionRepository, Clock.systemUTC());
    }

    BillingAccessGuard(BillingSubscriptionRepository subscriptionRepository, Clock clock) {
        this.subscriptionRepository = subscriptionRepository;
        this.clock = clock;
    }

    @Override
    public BillingSnapshot snapshotFor(UUID farmId) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        BillingSubscription subscription = subscriptionRepository.findByFarmId(farmId)
                .or(() -> subscriptionRepository.findByAccountOwnerFarmId(farmId))
                .orElseGet(() -> resolveTrialSubscription(farmId, now));
        return BillingSnapshot.from(subscription, now);
    }

    private BillingSubscription resolveTrialSubscription(UUID farmId, OffsetDateTime now) {
        OffsetDateTime trialEnd = subscriptionRepository.findFarmCreatedAt(farmId)
                .map(createdAt -> createdAt.plusDays(30))
                .orElse(null);
        if (trialEnd != null && !now.isAfter(trialEnd)) {
            return BillingSubscription.trialing(farmId, trialEnd);
        }
        return BillingSubscription.paymentRequired(farmId);
    }

    @Override
    public void requireWriteAllowed(UUID farmId) {
        BillingSnapshot snapshot = snapshotFor(farmId);
        if (!snapshot.canWrite()) {
            throw new BillingAccessDeniedException(snapshot);
        }
    }

    @Override
    public void requireWhatsappAllowed(UUID farmId) {
        BillingSnapshot snapshot = snapshotFor(farmId);
        if (!snapshot.canUseWhatsapp()) {
            throw new BillingAccessDeniedException(snapshot);
        }
    }
}
