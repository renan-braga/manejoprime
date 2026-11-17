package br.com.curralzap.backend.shared.application;

import java.util.List;

public record ImportResultView(
        int imported,
        int skipped,
        List<ImportRowError> errors
) {

    public record ImportRowError(int row, String message) {}
}
