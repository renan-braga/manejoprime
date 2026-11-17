package br.com.curralzap.backend.shared.api;

import br.com.curralzap.backend.shared.application.ImportResultView;

import java.util.List;

public record ImportResultApiResponse(
        int imported,
        int skipped,
        List<ImportRowError> errors
) {

    public record ImportRowError(int row, String message) {}

    public static ImportResultApiResponse from(ImportResultView view) {
        return new ImportResultApiResponse(
                view.imported(),
                view.skipped(),
                view.errors().stream()
                        .map(e -> new ImportRowError(e.row(), e.message()))
                        .toList()
        );
    }
}
