package br.com.curralzap.backend.vaccines.application;

import java.util.Map;

public class InvalidVaccineMutationException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public InvalidVaccineMutationException(Map<String, String> fieldErrors) {
        super("Dados invalidos.");
        this.fieldErrors = fieldErrors;
    }

    public Map<String, String> fieldErrors() {
        return fieldErrors;
    }
}
