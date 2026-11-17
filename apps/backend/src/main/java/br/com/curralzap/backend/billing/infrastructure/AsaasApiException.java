package br.com.curralzap.backend.billing.infrastructure;

class AsaasApiException extends RuntimeException {

    AsaasApiException(String message) {
        super(message);
    }

    AsaasApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
