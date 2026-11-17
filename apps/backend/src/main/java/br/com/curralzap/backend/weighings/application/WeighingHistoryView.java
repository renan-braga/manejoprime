package br.com.curralzap.backend.weighings.application;

import java.util.List;

public record WeighingHistoryView(
        List<WeighingHistoryItemView> items,
        long total,
        int limit,
        int offset
) {
}
