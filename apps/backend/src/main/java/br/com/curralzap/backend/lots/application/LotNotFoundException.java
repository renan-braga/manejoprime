package br.com.curralzap.backend.lots.application;

public class LotNotFoundException extends RuntimeException {

    public LotNotFoundException() {
        super("Lot was not found.");
    }
}
