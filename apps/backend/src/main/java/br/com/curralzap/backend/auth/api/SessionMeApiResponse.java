package br.com.curralzap.backend.auth.api;

import br.com.curralzap.backend.auth.application.SessionMeView;
import br.com.curralzap.backend.billing.application.BillingSnapshot;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

record SessionMeApiResponse(
        UUID userId,
        String name,
        String phone,
        String email,
        String role,
        UUID farmId,
        String farmName,
        List<String> permissions,
        BillingApiResponse billing,
        int farmCount,
        long animalCount,
        int userCount
) {

    static SessionMeApiResponse from(SessionMeView session) {
        return new SessionMeApiResponse(
                session.userId(),
                session.name(),
                session.phone(),
                session.email(),
                session.role(),
                session.farmId(),
                session.farmName(),
                session.permissions(),
                BillingApiResponse.from(session.billing()),
                session.farmCount(),
                session.animalCount(),
                session.userCount()
        );
    }

    record BillingApiResponse(
            String plan,
            String status,
            OffsetDateTime currentPeriodEnd,
            OffsetDateTime graceUntil,
            boolean billingExempt,
            String exemptionReason,
            boolean canWrite,
            boolean canUseWhatsapp
    ) {

        static BillingApiResponse from(BillingSnapshot snapshot) {
            return new BillingApiResponse(
                    snapshot.plan(),
                    snapshot.status(),
                    snapshot.currentPeriodEnd(),
                    snapshot.graceUntil(),
                    snapshot.billingExempt(),
                    snapshot.exemptionReason(),
                    snapshot.canWrite(),
                    snapshot.canUseWhatsapp()
            );
        }
    }
}
