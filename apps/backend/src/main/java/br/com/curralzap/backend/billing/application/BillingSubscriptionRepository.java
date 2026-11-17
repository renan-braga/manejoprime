package br.com.curralzap.backend.billing.application;

import br.com.curralzap.backend.billing.domain.BillingSubscription;
import br.com.curralzap.backend.billing.domain.BillingSubscriptionStatus;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface BillingSubscriptionRepository {

    Optional<BillingSubscription> findByFarmId(UUID farmId);

    /** Finds the billing subscription for the account owner of the given farm (via farms.owner_user_id). */
    Optional<BillingSubscription> findByAccountOwnerFarmId(UUID farmId);

    Optional<OffsetDateTime> findFarmCreatedAt(UUID farmId);

    Optional<BillingSubscription> findByProviderAndSubscriptionId(String provider, String providerSubscriptionId);

    void updateStatus(
            UUID id,
            BillingSubscriptionStatus status,
            OffsetDateTime currentPeriodEnd,
            OffsetDateTime graceUntil,
            OffsetDateTime suspendedAt,
            OffsetDateTime canceledAt,
            OffsetDateTime updatedAt
    );

    void upsertForCheckout(
            UUID farmId,
            String plan,
            String provider,
            String providerCustomerId,
            String providerSubscriptionId,
            OffsetDateTime now
    );

    void activateDirectCharge(UUID farmId, OffsetDateTime currentPeriodEnd, OffsetDateTime now);
}
