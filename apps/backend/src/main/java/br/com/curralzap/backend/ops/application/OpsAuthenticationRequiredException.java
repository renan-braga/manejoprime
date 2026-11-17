package br.com.curralzap.backend.ops.application;

public class OpsAuthenticationRequiredException extends RuntimeException {

    public OpsAuthenticationRequiredException() {
        super("Ops authentication required.");
    }
}
