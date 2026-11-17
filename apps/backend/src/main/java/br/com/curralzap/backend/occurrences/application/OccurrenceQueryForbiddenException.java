package br.com.curralzap.backend.occurrences.application;

public final class OccurrenceQueryForbiddenException extends RuntimeException {

    public OccurrenceQueryForbiddenException() {
        super("Occurrence history access is not allowed.");
    }
}
