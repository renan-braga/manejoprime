package br.com.curralzap.backend.offers.domain;

public enum OfferStatus {
    DRAFT("draft"),
    ACTIVE("active"),
    SOLD("sold"),
    CANCELLED("cancelled");

    private final String databaseValue;

    OfferStatus(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String databaseValue() {
        return databaseValue;
    }

    public String apiValue() {
        return databaseValue;
    }

    public static OfferStatus fromDatabaseValue(String value) {
        return switch (value) {
            case "draft" -> DRAFT;
            case "active" -> ACTIVE;
            case "sold" -> SOLD;
            case "cancelled" -> CANCELLED;
            default -> throw new IllegalArgumentException("Unsupported offer status: " + value);
        };
    }

    public static OfferStatus fromApiValue(String value) {
        return fromDatabaseValue(value.toLowerCase());
    }
}
