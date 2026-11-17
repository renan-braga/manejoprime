package br.com.curralzap.backend.animals.domain;

public enum AnimalSex {
    MALE("macho", "male"),
    FEMALE("femea", "female");

    private final String databaseValue;
    private final String apiValue;

    AnimalSex(String databaseValue, String apiValue) {
        this.databaseValue = databaseValue;
        this.apiValue = apiValue;
    }

    public String databaseValue() {
        return databaseValue;
    }

    public String apiValue() {
        return apiValue;
    }

    public static AnimalSex fromDatabaseValue(String databaseValue) {
        return switch (databaseValue) {
            case "macho" -> MALE;
            case "femea" -> FEMALE;
            default -> throw new IllegalArgumentException("Unsupported animal sex: " + databaseValue);
        };
    }

    public static AnimalSex fromApiValue(String apiValue) {
        return switch (apiValue) {
            case "male" -> MALE;
            case "female" -> FEMALE;
            default -> throw new IllegalArgumentException("Unsupported animal sex: " + apiValue);
        };
    }
}
