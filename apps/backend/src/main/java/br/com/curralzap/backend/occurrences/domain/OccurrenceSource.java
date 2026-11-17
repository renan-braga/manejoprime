package br.com.curralzap.backend.occurrences.domain;

public enum OccurrenceSource {
    APP("app"),
    WHATSAPP("whatsapp");

    private final String databaseValue;

    OccurrenceSource(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String databaseValue() {
        return databaseValue;
    }

    public static OccurrenceSource fromDatabaseValue(String value) {
        for (OccurrenceSource source : values()) {
            if (source.databaseValue.equals(value)) {
                return source;
            }
        }
        throw new IllegalArgumentException("Unknown occurrence source: " + value);
    }
}
