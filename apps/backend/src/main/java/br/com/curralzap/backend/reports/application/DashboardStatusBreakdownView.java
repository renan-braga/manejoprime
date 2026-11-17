package br.com.curralzap.backend.reports.application;

public record DashboardStatusBreakdownView(
        long active,
        long sold,
        long dead,
        long transferred
) {
}
