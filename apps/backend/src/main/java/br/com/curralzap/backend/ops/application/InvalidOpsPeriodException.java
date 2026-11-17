package br.com.curralzap.backend.ops.application;

public class InvalidOpsPeriodException extends RuntimeException {

    public InvalidOpsPeriodException(String period) {
        super("Invalid period '" + period + "'. Allowed values: 24h, 7d, 30d.");
    }
}
