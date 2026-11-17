package br.com.curralzap.backend.lots.api;

import br.com.curralzap.backend.lots.application.LotListItemView;
import br.com.curralzap.backend.lots.application.LotListView;

import java.util.List;
import java.util.UUID;

record LotListApiResponse(
        List<LotListItemApiResponse> items,
        long total,
        int limit,
        int offset
) {
    static LotListApiResponse from(LotListView lots) {
        return new LotListApiResponse(
                lots.items().stream().map(LotListItemApiResponse::from).toList(),
                lots.total(),
                lots.limit(),
                lots.offset()
        );
    }

    record LotListItemApiResponse(
            UUID id,
            String name,
            String purpose,
            String pastureArea,
            long animalsCount
    ) {
        static LotListItemApiResponse from(LotListItemView lot) {
            return new LotListItemApiResponse(
                    lot.id(),
                    lot.name(),
                    lot.purpose(),
                    lot.pastureArea(),
                    lot.animalsCount()
            );
        }
    }
}
