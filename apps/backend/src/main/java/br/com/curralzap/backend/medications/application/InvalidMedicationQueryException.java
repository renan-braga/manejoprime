package br.com.curralzap.backend.medications.application;

import java.util.Map;

public class InvalidMedicationQueryException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public InvalidMedicationQueryException(Map<String, String> fieldErrors) {
        super("Medication query is invalid.");
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public Map<String, String> fieldErrors() {
        return fieldErrors;
    }
}
