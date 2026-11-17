package br.com.curralzap.backend.billing.domain;

import java.time.OffsetDateTime;

public enum BillingSubscriptionStatus {
    FREE("free"),
    TRIALING("trialing"),
    ACTIVE("active"),
    PAST_DUE("past_due"),
    SUSPENDED("suspended"),
    CANCELED("canceled"),
    PAYMENT_REQUIRED("payment_required");

    private final String databaseValue;

    BillingSubscriptionStatus(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String databaseValue() {
        return databaseValue;
    }

    public static BillingSubscriptionStatus fromDatabaseValue(String value) {
        for (BillingSubscriptionStatus status : values()) {
            if (status.databaseValue.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unsupported billing subscription status: " + value);
    }

    public boolean allowsWrite(OffsetDateTime now, OffsetDateTime graceUntil) {
        return switch (this) {
            case FREE, TRIALING, ACTIVE -> true;
            case PAST_DUE -> graceUntil != null && !now.isAfter(graceUntil);
            case SUSPENDED, CANCELED, PAYMENT_REQUIRED -> false;
        };
    }
}
