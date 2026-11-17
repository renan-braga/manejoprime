package br.com.curralzap.backend.farms.application;

import java.util.Map;

public class InvalidFarmRegistrationException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public InvalidFarmRegistrationException(Map<String, String> fieldErrors) {
        super("Dados de registro inválidos.");
        this.fieldErrors = fieldErrors;
    }

    public Map<String, String> fieldErrors() {
        return fieldErrors;
    }
}
