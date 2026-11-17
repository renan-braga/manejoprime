package br.com.curralzap.backend.medications.application;

public class MedicationNotFoundException extends RuntimeException {

    public MedicationNotFoundException() {
        super("Medication not found.");
    }
}
