package br.com.curralzap.backend.auth.application;

public class SessionReplacedException extends RuntimeException {

    public SessionReplacedException() {
        super("Session was replaced by a newer login.");
    }
}
