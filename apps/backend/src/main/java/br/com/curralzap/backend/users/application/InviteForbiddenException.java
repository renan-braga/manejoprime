package br.com.curralzap.backend.users.application;

public class InviteForbiddenException extends RuntimeException {

    public InviteForbiddenException() {
        super("Acesso negado.");
    }
}
