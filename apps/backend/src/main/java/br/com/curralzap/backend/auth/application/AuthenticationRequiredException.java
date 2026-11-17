package br.com.curralzap.backend.auth.application;

public class AuthenticationRequiredException extends RuntimeException {

    public AuthenticationRequiredException() {
        super("Authentication is required.");
    }
}
