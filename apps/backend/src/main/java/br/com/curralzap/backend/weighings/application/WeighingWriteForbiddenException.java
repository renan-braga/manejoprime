package br.com.curralzap.backend.weighings.application;

public class WeighingWriteForbiddenException extends RuntimeException {

    public WeighingWriteForbiddenException() {
        super("You do not have permission to create weighings.");
    }
}
