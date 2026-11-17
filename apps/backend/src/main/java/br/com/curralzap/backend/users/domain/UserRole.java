package br.com.curralzap.backend.users.domain;

public enum UserRole {
    ADMIN("admin"),
    VETERINARIAN("veterinarian"),
    WORKER("worker");

    private final String databaseValue;

    UserRole(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public static UserRole fromDatabaseValue(String value) {
        for (UserRole role : values()) {
            if (role.databaseValue.equals(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unsupported user role: " + value);
    }

    public static UserRole fromApiValue(String value) {
        return fromDatabaseValue(value);
    }

    public String databaseValue() {
        return databaseValue;
    }
}
