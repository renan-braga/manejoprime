package br.com.curralzap.backend.feeding.api;

import br.com.curralzap.backend.feeding.application.FeedingListView;

import java.util.List;

record FeedingListApiResponse(
        List<FeedingApiResponse> items,
        long total,
        int limit,
        int offset
) {
    static FeedingListApiResponse from(FeedingListView feedings) {
        return new FeedingListApiResponse(
                feedings.items().stream().map(FeedingApiResponse::from).toList(),
                feedings.total(),
                feedings.limit(),
                feedings.offset()
        );
    }
}
