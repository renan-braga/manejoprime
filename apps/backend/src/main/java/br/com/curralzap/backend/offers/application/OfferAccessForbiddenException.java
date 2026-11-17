package br.com.curralzap.backend.offers.application;

public class OfferAccessForbiddenException extends RuntimeException {

    public OfferAccessForbiddenException() {
        super("Access to offers is forbidden.");
    }
}
