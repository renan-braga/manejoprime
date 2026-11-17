package br.com.curralzap.backend.occurrences.application;

public class OccurrenceNotFoundException extends RuntimeException {

    public OccurrenceNotFoundException() {
        super("Occurrence not found.");
    }
}
