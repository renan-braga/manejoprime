package br.com.curralzap.backend.occurrences.domain;

public enum OccurrenceType {
    HEALTH("health"),
    BEHAVIOR("behavior"),
    INJURY("injury"),
    PARASITE("parasite"),
    REPRODUCTION("reproduction"),
    NUTRITION("nutrition"),
    OTHER("other");

    private final String databaseValue;

    OccurrenceType(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String databaseValue() {
        return databaseValue;
    }

    public static OccurrenceType fromDatabaseValue(String value) {
        for (OccurrenceType type : values()) {
            if (type.databaseValue.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown occurrence type: " + value);
    }
}
