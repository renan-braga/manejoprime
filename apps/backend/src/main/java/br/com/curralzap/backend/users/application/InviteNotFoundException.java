package br.com.curralzap.backend.users.application;

public class InviteNotFoundException extends RuntimeException {

    public InviteNotFoundException() {
        super("Convite não encontrado.");
    }
}
