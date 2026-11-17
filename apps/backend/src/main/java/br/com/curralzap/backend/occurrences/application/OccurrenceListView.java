package br.com.curralzap.backend.occurrences.application;

import java.util.List;

public record OccurrenceListView(
        List<OccurrenceListItemView> items,
        long total,
        int limit,
        int offset
) {
}
