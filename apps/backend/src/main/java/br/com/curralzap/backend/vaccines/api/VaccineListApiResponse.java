package br.com.curralzap.backend.vaccines.api;

import br.com.curralzap.backend.vaccines.application.VaccineListView;

import java.util.List;

record VaccineListApiResponse(
        List<VaccineApiResponse> items,
        long total,
        int limit,
        int offset
) {
    static VaccineListApiResponse from(VaccineListView vaccines) {
        return new VaccineListApiResponse(
                vaccines.items().stream().map(VaccineApiResponse::from).toList(),
                vaccines.total(),
                vaccines.limit(),
                vaccines.offset()
        );
    }
}
