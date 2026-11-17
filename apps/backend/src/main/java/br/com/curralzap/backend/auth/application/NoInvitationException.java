package br.com.curralzap.backend.auth.application;

public class NoInvitationException extends RuntimeException {

    public NoInvitationException() {
        super("No pending invitation found for this account.");
    }
}
