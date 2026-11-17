package br.com.curralzap.backend.vaccines.application;

public class VaccineNotFoundException extends RuntimeException {

    public VaccineNotFoundException() {
        super("Vaccine not found.");
    }
}
