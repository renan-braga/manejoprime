package br.com.curralzap.backend.weighings.application;

import java.util.Map;

public final class InvalidWeighingQueryException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public InvalidWeighingQueryException(Map<String, String> fieldErrors) {
        super("Dados invalidos.");
        this.fieldErrors = fieldErrors;
    }

    public Map<String, String> fieldErrors() {
        return fieldErrors;
    }
}
