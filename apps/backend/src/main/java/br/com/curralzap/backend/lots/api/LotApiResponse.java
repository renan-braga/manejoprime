package br.com.curralzap.backend.lots.api;

import br.com.curralzap.backend.lots.application.LotDetailView;

import java.util.UUID;

record LotApiResponse(
        UUID id,
        UUID farmId,
        String name,
        String purpose,
        String pastureArea,
        long animalsCount
) {
    static LotApiResponse from(LotDetailView lot) {
        return new LotApiResponse(
                lot.id(),
                lot.farmId(),
                lot.name(),
                lot.purpose(),
                lot.pastureArea(),
                lot.animalsCount()
        );
    }
}
