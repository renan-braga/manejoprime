package br.com.curralzap.backend.billing.application;

public class BillingGatewayException extends RuntimeException {

    public enum Kind { NOT_CONFIGURED, CLIENT_ERROR, SERVER_ERROR }

    private final Kind kind;

    public BillingGatewayException(String message) {
        super(message);
        this.kind = Kind.SERVER_ERROR;
    }

    public BillingGatewayException(String message, Throwable cause) {
        super(message, cause);
        this.kind = Kind.SERVER_ERROR;
    }

    public BillingGatewayException(String message, Kind kind) {
        super(message);
        this.kind = kind;
    }

    public BillingGatewayException(String message, Kind kind, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }
}
