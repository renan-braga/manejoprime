package br.com.curralzap.backend.medications.domain;

public enum MedicationSource {
    APP("app"),
    WHATSAPP("whatsapp");

    private final String databaseValue;

    MedicationSource(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String databaseValue() {
        return databaseValue;
    }

    public String apiValue() {
        return databaseValue;
    }

    public static MedicationSource fromDatabaseValue(String value) {
        for (MedicationSource source : values()) {
            if (source.databaseValue.equals(value)) {
                return source;
            }
        }
        throw new IllegalArgumentException("Unknown medication source: " + value);
    }
}
