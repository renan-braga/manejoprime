package br.com.curralzap.backend.occurrences.application;

public class OccurrenceActionForbiddenException extends RuntimeException {

    public OccurrenceActionForbiddenException() {
        super("Occurrence action is not allowed.");
    }
}
