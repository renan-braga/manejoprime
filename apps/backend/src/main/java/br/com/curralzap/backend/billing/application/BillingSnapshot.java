package br.com.curralzap.backend.billing.application;

import br.com.curralzap.backend.billing.domain.BillingSubscription;

import java.time.OffsetDateTime;

public record BillingSnapshot(
        String plan,
        String status,
        OffsetDateTime currentPeriodEnd,
        OffsetDateTime graceUntil,
        boolean billingExempt,
        String exemptionReason,
        boolean canWrite,
        boolean canUseWhatsapp
) {

    public static BillingSnapshot from(BillingSubscription subscription, OffsetDateTime now) {
        return new BillingSnapshot(
                subscription.plan(),
                subscription.status().databaseValue(),
                subscription.currentPeriodEnd(),
                subscription.graceUntil(),
                subscription.billingExempt(),
                subscription.exemptionReason(),
                subscription.canWrite(now),
                subscription.canUseWhatsapp(now)
        );
    }
}
