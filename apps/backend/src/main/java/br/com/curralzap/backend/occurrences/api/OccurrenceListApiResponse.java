package br.com.curralzap.backend.occurrences.api;

import br.com.curralzap.backend.occurrences.application.OccurrenceListView;

import java.util.List;

record OccurrenceListApiResponse(
        List<OccurrenceListItemApiResponse> items,
        long total,
        int limit,
        int offset
) {
    static OccurrenceListApiResponse from(OccurrenceListView list) {
        return new OccurrenceListApiResponse(
                list.items().stream().map(OccurrenceListItemApiResponse::from).toList(),
                list.total(),
                list.limit(),
                list.offset()
        );
    }
}
