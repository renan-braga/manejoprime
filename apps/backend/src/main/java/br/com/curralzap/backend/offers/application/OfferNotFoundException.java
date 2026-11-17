package br.com.curralzap.backend.offers.application;

public class OfferNotFoundException extends RuntimeException {

    public OfferNotFoundException() {
        super("Offer was not found.");
    }
}
