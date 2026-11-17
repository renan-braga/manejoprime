package br.com.curralzap.backend.auth.domain;

public enum SessionRevocationReason {
    LOGGED_OUT("logged_out"),
    REPLACED_BY_NEW_LOGIN("replaced_by_new_login");

    private final String databaseValue;

    SessionRevocationReason(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String databaseValue() {
        return databaseValue;
    }

    public static SessionRevocationReason fromDatabaseValue(String value) {
        for (SessionRevocationReason reason : values()) {
            if (reason.databaseValue.equals(value)) {
                return reason;
            }
        }
        throw new IllegalArgumentException("Unsupported session revocation reason: " + value);
    }
}
