package br.com.curralzap.backend.weighings.api;

import br.com.curralzap.backend.weighings.application.WeighingHistoryView;

import java.util.List;

record WeighingHistoryApiResponse(
        List<WeighingApiResponse> items,
        long total,
        int limit,
        int offset
) {
    static WeighingHistoryApiResponse from(WeighingHistoryView history) {
        return new WeighingHistoryApiResponse(
                history.items().stream().map(WeighingApiResponse::from).toList(),
                history.total(),
                history.limit(),
                history.offset()
        );
    }
}
