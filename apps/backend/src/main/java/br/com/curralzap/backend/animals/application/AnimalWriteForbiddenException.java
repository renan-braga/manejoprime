package br.com.curralzap.backend.animals.application;

public class AnimalWriteForbiddenException extends RuntimeException {

    public AnimalWriteForbiddenException() {
        super("You do not have permission to modify animal records.");
    }
}
