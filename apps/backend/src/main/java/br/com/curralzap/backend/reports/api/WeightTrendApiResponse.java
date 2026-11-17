package br.com.curralzap.backend.reports.api;

import br.com.curralzap.backend.reports.application.WeightTrendItemView;

import java.math.BigDecimal;
import java.time.LocalDate;

record WeightTrendApiResponse(LocalDate date, BigDecimal avgWeight, BigDecimal avgDailyGain) {

    static WeightTrendApiResponse from(WeightTrendItemView item) {
        return new WeightTrendApiResponse(item.date(), item.avgWeight(), item.avgDailyGain());
    }
}
