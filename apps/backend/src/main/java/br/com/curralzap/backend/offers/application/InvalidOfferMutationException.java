package br.com.curralzap.backend.offers.application;

import java.util.Map;

public class InvalidOfferMutationException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public InvalidOfferMutationException(Map<String, String> fieldErrors) {
        super("Dados invalidos.");
        this.fieldErrors = fieldErrors;
    }

    public Map<String, String> fieldErrors() {
        return fieldErrors;
    }
}
