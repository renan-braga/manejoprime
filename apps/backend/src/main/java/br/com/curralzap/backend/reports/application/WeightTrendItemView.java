package br.com.curralzap.backend.reports.application;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WeightTrendItemView(LocalDate date, BigDecimal avgWeight, BigDecimal avgDailyGain) {
}
