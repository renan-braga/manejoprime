package br.com.curralzap.backend.lots.api;

import br.com.curralzap.backend.lots.application.LotUpsertInput;

record LotUpsertApiRequest(
        String name,
        String purpose,
        String pastureArea
) {
    LotUpsertInput toInput() {
        return LotUpsertInput.fromApi(name, purpose, pastureArea);
    }
}
