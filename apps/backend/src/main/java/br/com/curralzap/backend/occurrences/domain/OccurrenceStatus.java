package br.com.curralzap.backend.occurrences.domain;

public enum OccurrenceStatus {
    OPEN("open"),
    IN_FOLLOW_UP("in_follow_up"),
    RESOLVED("resolved");

    private final String databaseValue;

    OccurrenceStatus(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String databaseValue() {
        return databaseValue;
    }

    public String apiValue() {
        return databaseValue;
    }

    public static OccurrenceStatus fromDatabaseValue(String value) {
        for (OccurrenceStatus status : values()) {
            if (status.databaseValue.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown occurrence status: " + value);
    }
}
