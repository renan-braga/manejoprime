package br.com.curralzap.backend.farms.application;

public class FarmSwitchForbiddenException extends RuntimeException {
    public FarmSwitchForbiddenException() {
        super("User does not have access to the requested farm.");
    }
}
