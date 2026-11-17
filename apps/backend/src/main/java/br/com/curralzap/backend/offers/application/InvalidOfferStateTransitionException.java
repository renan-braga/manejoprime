package br.com.curralzap.backend.offers.application;

import br.com.curralzap.backend.offers.domain.OfferStatus;

public class InvalidOfferStateTransitionException extends RuntimeException {

    public InvalidOfferStateTransitionException(OfferStatus from, OfferStatus to) {
        super("Cannot transition offer from " + from.apiValue() + " to " + to.apiValue() + ".");
    }
}
