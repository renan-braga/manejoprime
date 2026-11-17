package br.com.curralzap.backend.auth.application;

public class OAuthTokenVerificationException extends RuntimeException {

    public OAuthTokenVerificationException(String message) {
        super(message);
    }

    public OAuthTokenVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
