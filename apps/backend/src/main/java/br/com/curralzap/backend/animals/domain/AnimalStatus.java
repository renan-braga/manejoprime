package br.com.curralzap.backend.animals.domain;

public enum AnimalStatus {
    ACTIVE("ativo", "active"),
    SOLD("vendido", "sold"),
    DEAD("morto", "dead"),
    TRANSFERRED("transferido", "transferred");

    private final String databaseValue;
    private final String apiValue;

    AnimalStatus(String databaseValue, String apiValue) {
        this.databaseValue = databaseValue;
        this.apiValue = apiValue;
    }

    public String databaseValue() {
        return databaseValue;
    }

    public String apiValue() {
        return apiValue;
    }

    public static AnimalStatus fromDatabaseValue(String databaseValue) {
        return switch (databaseValue) {
            case "ativo" -> ACTIVE;
            case "vendido" -> SOLD;
            case "morto" -> DEAD;
            case "transferido" -> TRANSFERRED;
            default -> throw new IllegalArgumentException("Unsupported animal status: " + databaseValue);
        };
    }

    public static AnimalStatus fromApiValue(String apiValue) {
        return switch (apiValue) {
            case "active" -> ACTIVE;
            case "sold" -> SOLD;
            case "dead" -> DEAD;
            case "transferred" -> TRANSFERRED;
            default -> throw new IllegalArgumentException("Unsupported animal status: " + apiValue);
        };
    }
}
