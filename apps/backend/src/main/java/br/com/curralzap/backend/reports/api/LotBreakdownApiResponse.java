package br.com.curralzap.backend.reports.api;

import br.com.curralzap.backend.reports.application.LotBreakdownItemView;

import java.math.BigDecimal;
import java.util.UUID;

record LotBreakdownApiResponse(UUID lotId, String lotName, long animalCount, BigDecimal avgWeight) {

    static LotBreakdownApiResponse from(LotBreakdownItemView item) {
        return new LotBreakdownApiResponse(item.lotId(), item.lotName(), item.headCount(), item.avgWeight());
    }
}
