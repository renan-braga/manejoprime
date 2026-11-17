package br.com.curralzap.backend.reports.application;

import java.math.BigDecimal;

public record DashboardAggregateView(
        long totalAnimals,
        long prevTotalAnimals,
        long lotsCount,
        long prevLotsCount,
        long activeUsers,
        long prevActiveUsers,
        BigDecimal avgDailyGain,
        BigDecimal prevAvgDailyGain,
        DashboardStatusBreakdownView statusBreakdown,
        long prevActiveAnimals
) {
}
