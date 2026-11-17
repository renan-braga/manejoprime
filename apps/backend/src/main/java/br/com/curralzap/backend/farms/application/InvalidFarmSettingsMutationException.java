package br.com.curralzap.backend.farms.application;

import java.util.Map;

public class InvalidFarmSettingsMutationException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public InvalidFarmSettingsMutationException(Map<String, String> fieldErrors) {
        super("Dados invalidos.");
        this.fieldErrors = fieldErrors;
    }

    public Map<String, String> fieldErrors() {
        return fieldErrors;
    }
}
