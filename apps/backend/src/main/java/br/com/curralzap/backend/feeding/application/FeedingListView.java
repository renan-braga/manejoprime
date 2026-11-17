package br.com.curralzap.backend.feeding.application;

import java.util.List;

public record FeedingListView(
        List<FeedingView> items,
        long total,
        int limit,
        int offset
) {
}
