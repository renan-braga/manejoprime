package br.com.curralzap.backend.medications.application;

import java.util.Map;

public class InvalidMedicationMutationException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public InvalidMedicationMutationException(Map<String, String> fieldErrors) {
        super("Medication payload is invalid.");
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public Map<String, String> fieldErrors() {
        return fieldErrors;
    }
}
