package br.com.curralzap.backend.shared.domain;

import java.util.UUID;

public final class AppendFirstWriteGuard {

    private AppendFirstWriteGuard() {
    }

    public static void requireRepositoryAssignedId(String recordFamily, UUID recordId) {
        if (recordId != null) {
            throw new AppendFirstMutationNotAllowedException(recordFamily, recordId);
        }
    }

    public static void requireUnusedId(String recordFamily, UUID recordId, boolean exists) {
        if (recordId != null && exists) {
            throw new AppendFirstMutationNotAllowedException(recordFamily, recordId);
        }
    }
}
