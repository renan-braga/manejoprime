package br.com.curralzap.backend.reports.api;

import br.com.curralzap.backend.reports.application.DashboardAlertView;
import br.com.curralzap.backend.reports.application.DashboardStatusBreakdownView;
import br.com.curralzap.backend.reports.application.DashboardView;

import java.math.BigDecimal;
import java.util.List;

record DashboardApiResponse(
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
        DashboardStatusBreakdownApiResponse statusBreakdown,
        List<DashboardAlertApiResponse> alerts,
        List<String> shortcuts
) {

    static DashboardApiResponse from(DashboardView dashboard) {
        return new DashboardApiResponse(
                dashboard.totalAnimals(),
                dashboard.totalAnimalsChangePct(),
                dashboard.activeAnimals(),
                dashboard.activeAnimalsChangePct(),
                dashboard.lotsCount(),
                dashboard.lotsCountChangePct(),
                dashboard.activeUsers(),
                dashboard.activeUsersChangePct(),
                dashboard.avgDailyGain(),
                dashboard.avgDailyGainChangePct(),
                DashboardStatusBreakdownApiResponse.from(dashboard.statusBreakdown()),
                dashboard.alerts().stream().map(DashboardAlertApiResponse::from).toList(),
                dashboard.shortcuts()
        );
    }

    record DashboardStatusBreakdownApiResponse(long active, long sold, long dead, long transferred) {
        static DashboardStatusBreakdownApiResponse from(DashboardStatusBreakdownView v) {
            return new DashboardStatusBreakdownApiResponse(v.active(), v.sold(), v.dead(), v.transferred());
        }
    }

    record DashboardAlertApiResponse(String type, String title, long count) {
        static DashboardAlertApiResponse from(DashboardAlertView alert) {
            return new DashboardAlertApiResponse(alert.type(), alert.title(), alert.count());
        }
    }
}
