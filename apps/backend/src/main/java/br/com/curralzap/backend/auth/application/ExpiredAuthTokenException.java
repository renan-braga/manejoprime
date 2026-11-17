package br.com.curralzap.backend.auth.application;

public class ExpiredAuthTokenException extends RuntimeException {

    public ExpiredAuthTokenException() {
        super("Auth token is expired.");
    }
}
