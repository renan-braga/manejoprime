package br.com.curralzap.backend.shared.application;

public record DependencyStatus(
        String name,
        boolean up,
        String detail
) {

    public String status() {
        return up ? "UP" : "DOWN";
    }
}
