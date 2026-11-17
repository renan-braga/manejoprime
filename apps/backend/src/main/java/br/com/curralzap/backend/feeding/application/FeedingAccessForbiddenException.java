package br.com.curralzap.backend.feeding.application;

public class FeedingAccessForbiddenException extends RuntimeException {

    public FeedingAccessForbiddenException() {
        super("Forbidden");
    }
}
