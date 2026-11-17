package br.com.curralzap.backend.auth.domain;

public class InvalidPhoneFormatException extends RuntimeException {

    public InvalidPhoneFormatException() {
        super("Phone number format is invalid.");
    }
}
