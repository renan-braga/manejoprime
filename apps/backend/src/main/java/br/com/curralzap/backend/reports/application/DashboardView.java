package br.com.curralzap.backend.reports.application;

import java.math.BigDecimal;
import java.util.List;

public record DashboardView(
        long totalAnimals,
        double totalAnimalsChangePct,
        long activeAnimals,
        double activeAnimalsChangePct,
        long lotsCount,
        double lotsCountChangePct,
        long activeUsers,
        double activeUsersChangePct,
        BigDecimal avgDailyGain,
        double avgDailyGainChangePct,
        DashboardStatusBreakdownView statusBreakdown,
        List<DashboardAlertView> alerts,
        List<String> shortcuts
) {
}
