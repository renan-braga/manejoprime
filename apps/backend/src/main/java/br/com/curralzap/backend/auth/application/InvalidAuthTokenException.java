package br.com.curralzap.backend.auth.application;

public class InvalidAuthTokenException extends RuntimeException {

    public InvalidAuthTokenException() {
        super("Auth token is invalid.");
    }

    public InvalidAuthTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
