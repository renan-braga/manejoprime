package br.com.curralzap.backend.lots.application;

import java.util.Map;

public class InvalidLotUpsertException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public InvalidLotUpsertException(Map<String, String> fieldErrors) {
        super("Dados invalidos.");
        this.fieldErrors = fieldErrors;
    }

    public Map<String, String> fieldErrors() {
        return fieldErrors;
    }
}
