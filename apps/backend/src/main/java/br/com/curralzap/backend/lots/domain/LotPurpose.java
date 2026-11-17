package br.com.curralzap.backend.lots.domain;

public enum LotPurpose {
    BREEDING("breeding"),
    RAISING("raising"),
    FATTENING("fattening"),
    REPRODUCTION("reproduction");

    private final String databaseValue;

    LotPurpose(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String databaseValue() {
        return databaseValue;
    }

    public String apiValue() {
        return databaseValue;
    }

    public static LotPurpose fromApiValue(String value) {
        return switch (value.toLowerCase()) {
            case "breeding" -> BREEDING;
            case "raising" -> RAISING;
            case "fattening" -> FATTENING;
            case "reproduction" -> REPRODUCTION;
            default -> throw new IllegalArgumentException("Unsupported lot purpose: " + value);
        };
    }

    public static LotPurpose fromDatabaseValue(String value) {
        return switch (value) {
            case "breeding" -> BREEDING;
            case "raising" -> RAISING;
            case "fattening" -> FATTENING;
            case "reproduction" -> REPRODUCTION;
            default -> throw new IllegalArgumentException("Unsupported lot purpose: " + value);
        };
    }
}
