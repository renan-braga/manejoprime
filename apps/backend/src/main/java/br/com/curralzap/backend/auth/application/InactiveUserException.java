package br.com.curralzap.backend.auth.application;

public class InactiveUserException extends RuntimeException {

    public InactiveUserException() {
        super("User is inactive and cannot authenticate.");
    }
}
