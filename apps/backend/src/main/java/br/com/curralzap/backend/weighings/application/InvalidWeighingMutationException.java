package br.com.curralzap.backend.weighings.application;

import java.util.Map;

public class InvalidWeighingMutationException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public InvalidWeighingMutationException(Map<String, String> fieldErrors) {
        super("Dados invalidos.");
        this.fieldErrors = fieldErrors;
    }

    public Map<String, String> fieldErrors() {
        return fieldErrors;
    }
}
