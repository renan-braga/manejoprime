package br.com.curralzap.backend.feeding.domain;

public enum FeedingSource {
    APP("app"),
    WHATSAPP("whatsapp");

    private final String databaseValue;

    FeedingSource(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String databaseValue() {
        return databaseValue;
    }

    public String apiValue() {
        return databaseValue;
    }

    public static FeedingSource fromDatabaseValue(String value) {
        for (FeedingSource source : values()) {
            if (source.databaseValue.equals(value)) {
                return source;
            }
        }
        throw new IllegalArgumentException("Unknown feeding source: " + value);
    }
}
