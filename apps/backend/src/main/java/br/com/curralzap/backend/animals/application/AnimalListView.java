package br.com.curralzap.backend.animals.application;

import java.util.List;

public record AnimalListView(
        List<AnimalListItemView> items,
        long total,
        int limit,
        int offset
) {
}
