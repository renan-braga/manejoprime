package br.com.curralzap.backend.weighings.domain;

public enum WeighingSource {
    APP("app"),
    WHATSAPP("whatsapp"),
    IOT("iot");

    private final String value;

    WeighingSource(String value) {
        this.value = value;
    }

    public static WeighingSource fromDatabaseValue(String databaseValue) {
        for (WeighingSource source : values()) {
            if (source.value.equalsIgnoreCase(databaseValue)) {
                return source;
            }
        }
        throw new IllegalArgumentException("Unsupported weighing source: " + databaseValue);
    }

    public String apiValue() {
        return value;
    }

    public String databaseValue() {
        return value;
    }
}
