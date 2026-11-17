package br.com.curralzap.backend.auth.application;

public class ExpiredRefreshTokenException extends RuntimeException {

    public ExpiredRefreshTokenException() {
        super("Refresh token is expired.");
    }
}
