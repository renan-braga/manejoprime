package br.com.curralzap.backend.reports.application;

import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.auth.application.RolePermissionSummary;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class DashboardService {

    private final DashboardViewRepository dashboardViewRepository;

    public DashboardService(DashboardViewRepository dashboardViewRepository) {
        this.dashboardViewRepository = dashboardViewRepository;
    }

    public DashboardView load(AuthTokenClaims claims) {
        DashboardAggregateView agg = dashboardViewRepository.loadFor(claims.farmId());
        List<String> permissions = RolePermissionSummary.permissionsFor(claims.role());

        long activeAnimals = agg.statusBreakdown().active();

        return new DashboardView(
                agg.totalAnimals(),
                changePct(agg.totalAnimals(), agg.prevTotalAnimals()),
                activeAnimals,
                changePct(activeAnimals, agg.prevActiveAnimals()),
                agg.lotsCount(),
                changePct(agg.lotsCount(), agg.prevLotsCount()),
                agg.activeUsers(),
                changePct(agg.activeUsers(), agg.prevActiveUsers()),
                agg.avgDailyGain(),
                changePct(agg.avgDailyGain(), agg.prevAvgDailyGain()),
                agg.statusBreakdown(),
                List.of(),
                DashboardShortcutSummary.shortcutsFor(permissions)
        );
    }

    public List<WeightTrendItemView> loadWeightTrend(AuthTokenClaims claims) {
        List<String> permissions = RolePermissionSummary.permissionsFor(claims.role());
        if (!permissions.contains("dashboard.read")) {
            throw new DashboardReadForbiddenException();
        }
        return dashboardViewRepository.loadWeightTrend(claims.farmId());
    }

    public List<LotBreakdownItemView> loadLotBreakdown(AuthTokenClaims claims) {
        List<String> permissions = RolePermissionSummary.permissionsFor(claims.role());
        if (!permissions.contains("dashboard.read")) {
            throw new DashboardReadForbiddenException();
        }
        return dashboardViewRepository.loadLotBreakdown(claims.farmId());
    }

    private static double changePct(long current, long previous) {
        if (previous == 0) return 0.0;
        return Math.round(((double) (current - previous) / previous) * 1000.0) / 10.0;
    }

    private static double changePct(BigDecimal current, BigDecimal previous) {
        if (current == null || previous == null || previous.compareTo(BigDecimal.ZERO) == 0) return 0.0;
        double pct = (current.doubleValue() - previous.doubleValue()) / previous.doubleValue() * 100.0;
        return Math.round(pct * 10.0) / 10.0;
    }
}
