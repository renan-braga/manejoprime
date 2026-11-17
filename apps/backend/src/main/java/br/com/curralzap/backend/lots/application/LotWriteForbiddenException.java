package br.com.curralzap.backend.lots.application;

public class LotWriteForbiddenException extends RuntimeException {

    public LotWriteForbiddenException() {
        super("You do not have permission to modify lot records.");
    }
}
