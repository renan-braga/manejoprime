package br.com.curralzap.backend.vaccines.domain;

public enum VaccineDose {
    FIRST("primeira"),
    SECOND("segunda"),
    BOOSTER("reforco"),
    ANNUAL("anual");

    private final String databaseValue;

    VaccineDose(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String databaseValue() {
        return databaseValue;
    }

    public String apiValue() {
        return databaseValue;
    }

    public static VaccineDose fromDatabaseValue(String value) {
        for (VaccineDose dose : values()) {
            if (dose.databaseValue.equals(value)) {
                return dose;
            }
        }
        throw new IllegalArgumentException("Unknown vaccine dose: " + value);
    }
}
