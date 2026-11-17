package br.com.curralzap.backend.feeding.domain;

public enum FeedingType {
    PASTURE("pasto"),
    FEED("racao"),
    SILAGE("silagem"),
    SUPPLEMENT("suplemento"),
    MINERAL_SALT("sal_mineral"),
    MIXED("misto");

    private final String databaseValue;

    FeedingType(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String databaseValue() {
        return databaseValue;
    }

    public String apiValue() {
        return databaseValue;
    }

    public static FeedingType fromDatabaseValue(String value) {
        for (FeedingType type : values()) {
            if (type.databaseValue.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown feeding type: " + value);
    }
}
