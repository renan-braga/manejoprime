package br.com.curralzap.backend.animals.application;

public class AnimalNotFoundException extends RuntimeException {

    public AnimalNotFoundException() {
        super("Animal was not found.");
    }
}
