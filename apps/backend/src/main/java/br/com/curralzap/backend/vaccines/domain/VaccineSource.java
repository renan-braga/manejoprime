package br.com.curralzap.backend.vaccines.domain;

public enum VaccineSource {
    APP("app"),
    WHATSAPP("whatsapp");

    private final String databaseValue;

    VaccineSource(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String databaseValue() {
        return databaseValue;
    }

    public String apiValue() {
        return databaseValue;
    }

    public static VaccineSource fromDatabaseValue(String value) {
        for (VaccineSource source : values()) {
            if (source.databaseValue.equals(value)) {
                return source;
            }
        }
        throw new IllegalArgumentException("Unknown vaccine source: " + value);
    }
}
