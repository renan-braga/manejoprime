package br.com.curralzap.backend.vaccines.application;

public class VaccineAccessForbiddenException extends RuntimeException {

    public VaccineAccessForbiddenException() {
        super("Forbidden");
    }
}
