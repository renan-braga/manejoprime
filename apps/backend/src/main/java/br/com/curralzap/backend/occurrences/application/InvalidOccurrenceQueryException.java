package br.com.curralzap.backend.occurrences.application;

import java.util.Map;

public final class InvalidOccurrenceQueryException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public InvalidOccurrenceQueryException(Map<String, String> fieldErrors) {
        super("Occurrence query is invalid.");
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public Map<String, String> fieldErrors() {
        return fieldErrors;
    }
}
