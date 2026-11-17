package br.com.curralzap.backend.auth.application;

public class RevokedRefreshTokenException extends RuntimeException {

    public RevokedRefreshTokenException() {
        super("Refresh token is no longer valid.");
    }
}
