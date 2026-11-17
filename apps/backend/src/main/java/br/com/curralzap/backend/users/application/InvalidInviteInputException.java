package br.com.curralzap.backend.users.application;

import java.util.Map;

public class InvalidInviteInputException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public InvalidInviteInputException(Map<String, String> fieldErrors) {
        super("Dados inválidos.");
        this.fieldErrors = fieldErrors;
    }

    public Map<String, String> fieldErrors() {
        return fieldErrors;
    }
}
