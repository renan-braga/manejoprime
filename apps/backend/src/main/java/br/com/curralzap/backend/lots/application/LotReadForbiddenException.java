package br.com.curralzap.backend.lots.application;

public class LotReadForbiddenException extends RuntimeException {

    public LotReadForbiddenException() {
        super("You do not have permission to view lot records.");
    }
}
