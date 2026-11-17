package br.com.curralzap.backend.billing.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BillingSubscription(
        UUID id,
        UUID farmId,
        String provider,
        String providerCustomerId,
        String providerSubscriptionId,
        String plan,
        BillingSubscriptionStatus status,
        OffsetDateTime currentPeriodEnd,
        OffsetDateTime graceUntil,
        OffsetDateTime suspendedAt,
        OffsetDateTime canceledAt,
        boolean billingExempt,
        String exemptionReason
) {

    public static BillingSubscription free(UUID farmId) {
        return new BillingSubscription(
                null, farmId, null, null, null,
                "free", BillingSubscriptionStatus.FREE,
                null, null, null, null, false, null
        );
    }

    public static BillingSubscription trialing(UUID farmId, OffsetDateTime trialEndsAt) {
        return new BillingSubscription(
                null, farmId, null, null, null,
                "trialing", BillingSubscriptionStatus.TRIALING,
                trialEndsAt, null, null, null, false, null
        );
    }

    public static BillingSubscription paymentRequired(UUID farmId) {
        return new BillingSubscription(
                null, farmId, null, null, null,
                "free", BillingSubscriptionStatus.PAYMENT_REQUIRED,
                null, null, null, null, false, null
        );
    }

    public boolean canWrite(OffsetDateTime now) {
        return billingExempt || status.allowsWrite(now, graceUntil);
    }

    public boolean canUseWhatsapp(OffsetDateTime now) {
        return billingExempt || status.allowsWrite(now, graceUntil);
    }
}
