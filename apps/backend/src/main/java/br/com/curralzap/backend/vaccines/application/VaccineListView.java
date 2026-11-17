package br.com.curralzap.backend.vaccines.application;

import java.util.List;

public record VaccineListView(
        List<VaccineView> items,
        long total,
        int limit,
        int offset
) {
}
