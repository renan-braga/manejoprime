package br.com.curralzap.backend.weighings.application;

public class WeighingAnimalNotFoundException extends RuntimeException {

    public WeighingAnimalNotFoundException() {
        super("Animal not found.");
    }
}
