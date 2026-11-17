package br.com.curralzap.backend.reports.application;

public record DashboardAlertView(
        String type,
        String title,
        long count
) {
}
