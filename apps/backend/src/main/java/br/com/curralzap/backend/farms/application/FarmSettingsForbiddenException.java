package br.com.curralzap.backend.farms.application;

public class FarmSettingsForbiddenException extends RuntimeException {

    public FarmSettingsForbiddenException() {
        super("User does not have permission to manage farm settings.");
    }
}
