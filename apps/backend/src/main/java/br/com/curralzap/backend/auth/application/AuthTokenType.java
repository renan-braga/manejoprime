package br.com.curralzap.backend.auth.application;

public enum AuthTokenType {
    ACCESS("access"),
    REFRESH("refresh"),
    ONBOARDING("onboarding");

    private final String claimValue;

    AuthTokenType(String claimValue) {
        this.claimValue = claimValue;
    }

    public String claimValue() {
        return claimValue;
    }

    public static AuthTokenType fromClaimValue(String value) {
        for (AuthTokenType type : values()) {
            if (type.claimValue.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported auth token type: " + value);
    }
}
