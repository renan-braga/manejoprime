package br.com.curralzap.backend.medications.application;

public class MedicationAccessForbiddenException extends RuntimeException {

    public MedicationAccessForbiddenException() {
        super("You do not have permission to access medications.");
    }
}
