package br.com.curralzap.backend.auth.application;

public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("Refresh token is invalid.");
    }
}
