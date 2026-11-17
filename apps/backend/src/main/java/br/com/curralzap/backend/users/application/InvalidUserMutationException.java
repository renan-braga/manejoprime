package br.com.curralzap.backend.users.application;

import java.util.Map;

public class InvalidUserMutationException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public InvalidUserMutationException(Map<String, String> fieldErrors) {
        super("Dados invalidos.");
        this.fieldErrors = fieldErrors;
    }

    public Map<String, String> fieldErrors() {
        return fieldErrors;
    }
}
