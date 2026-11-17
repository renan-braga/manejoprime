package br.com.curralzap.backend.occurrences.application;

public class InvalidOccurrenceStateTransitionException extends RuntimeException {

    public InvalidOccurrenceStateTransitionException() {
        super("Occurrence state transition is invalid.");
    }
}
