package br.com.curralzap.backend.shared.domain;

import java.util.UUID;

public class AppendFirstMutationNotAllowedException extends RuntimeException {

    public AppendFirstMutationNotAllowedException(String recordFamily, UUID recordId) {
        super(recordFamily + " records are append-first and cannot be updated in place (id: " + recordId + ").");
    }
}
