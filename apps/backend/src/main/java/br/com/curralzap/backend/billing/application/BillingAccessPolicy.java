package br.com.curralzap.backend.billing.application;

import java.util.UUID;

public interface BillingAccessPolicy {

    BillingSnapshot snapshotFor(UUID farmId);

    void requireWriteAllowed(UUID farmId);

    void requireWhatsappAllowed(UUID farmId);

    static BillingAccessPolicy alwaysAllow() {
        return new BillingAccessPolicy() {
            @Override
            public BillingSnapshot snapshotFor(UUID farmId) {
                return new BillingSnapshot("free", "free", null, null, false, null, true, true);
            }

            @Override
            public void requireWriteAllowed(UUID farmId) {
            }

            @Override
            public void requireWhatsappAllowed(UUID farmId) {
            }
        };
    }
}
