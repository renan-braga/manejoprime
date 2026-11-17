package br.com.curralzap.backend.lots.application;

import java.util.List;

public record LotListView(
        List<LotListItemView> items,
        long total,
        int limit,
        int offset
) {
}
