package br.com.curralzap.backend.lots.application;

import java.util.Map;

public class InvalidLotQueryException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public InvalidLotQueryException(Map<String, String> fieldErrors) {
        super("Dados invalidos.");
        this.fieldErrors = fieldErrors;
    }

    public Map<String, String> fieldErrors() {
        return fieldErrors;
    }
}
