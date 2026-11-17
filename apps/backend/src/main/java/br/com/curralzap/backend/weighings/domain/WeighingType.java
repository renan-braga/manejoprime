package br.com.curralzap.backend.weighings.domain;

public enum WeighingType {
    ROUTINE("routine", "rotina"),
    ENTRY("entry", "entrada"),
    EXIT("exit", "saida"),
    WEANING("weaning", "desmame"),
    SLAUGHTER("slaughter", "abate");

    private final String apiValue;
    private final String databaseValue;

    WeighingType(String apiValue, String databaseValue) {
        this.apiValue = apiValue;
        this.databaseValue = databaseValue;
    }

    public static WeighingType fromApiValue(String apiValue) {
        for (WeighingType type : values()) {
            if (type.apiValue.equalsIgnoreCase(apiValue)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported weighing type: " + apiValue);
    }

    public static WeighingType fromDatabaseValue(String databaseValue) {
        for (WeighingType type : values()) {
            if (type.databaseValue.equalsIgnoreCase(databaseValue)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported weighing type: " + databaseValue);
    }

    public String apiValue() {
        return apiValue;
    }

    public String databaseValue() {
        return databaseValue;
    }
}
