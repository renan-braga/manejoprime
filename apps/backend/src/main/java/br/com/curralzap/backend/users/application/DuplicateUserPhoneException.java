package br.com.curralzap.backend.users.application;

import java.util.Map;

public class DuplicateUserPhoneException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public DuplicateUserPhoneException() {
        super("Phone number is already assigned to another user.");
        this.fieldErrors = Map.of("phone", getMessage());
    }

    public Map<String, String> fieldErrors() {
        return fieldErrors;
    }
}
