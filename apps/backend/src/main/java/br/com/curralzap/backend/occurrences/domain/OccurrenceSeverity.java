package br.com.curralzap.backend.occurrences.domain;

public enum OccurrenceSeverity {
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high"),
    CRITICAL("critical");

    private final String databaseValue;

    OccurrenceSeverity(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String databaseValue() {
        return databaseValue;
    }

    public static OccurrenceSeverity fromDatabaseValue(String value) {
        for (OccurrenceSeverity severity : values()) {
            if (severity.databaseValue.equals(value)) {
                return severity;
            }
        }
        throw new IllegalArgumentException("Unknown occurrence severity: " + value);
    }
}
