package br.com.curralzap.backend.farms.application;

public class FarmSettingsNotFoundException extends RuntimeException {

    public FarmSettingsNotFoundException() {
        super("Farm settings not found.");
    }
}
