package br.com.curralzap.backend.feeding.application;

import java.util.Map;

public class InvalidFeedingMutationException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public InvalidFeedingMutationException(Map<String, String> fieldErrors) {
        super("Dados invalidos.");
        this.fieldErrors = fieldErrors;
    }

    public Map<String, String> fieldErrors() {
        return fieldErrors;
    }
}
