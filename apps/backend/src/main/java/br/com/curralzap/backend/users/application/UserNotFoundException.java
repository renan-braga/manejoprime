package br.com.curralzap.backend.users.application;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException() {
        super("User not found.");
    }
}
