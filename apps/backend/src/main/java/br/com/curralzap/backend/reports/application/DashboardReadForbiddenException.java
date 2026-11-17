package br.com.curralzap.backend.reports.application;

public class DashboardReadForbiddenException extends RuntimeException {

    public DashboardReadForbiddenException() {
        super("You do not have permission to read dashboard data.");
    }
}
