package br.com.curralzap.backend.animals.application;

import java.util.Map;

public class InvalidAnimalQueryException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public InvalidAnimalQueryException(Map<String, String> fieldErrors) {
        super("Dados invalidos.");
        this.fieldErrors = fieldErrors;
    }

    public Map<String, String> fieldErrors() {
        return fieldErrors;
    }
}
