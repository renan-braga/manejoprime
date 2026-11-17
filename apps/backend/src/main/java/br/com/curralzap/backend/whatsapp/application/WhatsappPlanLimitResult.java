package br.com.curralzap.backend.whatsapp.application;

public record WhatsappPlanLimitResult(
        boolean permitted,
        String reason
) {

    public static WhatsappPlanLimitResult allowed() {
        return new WhatsappPlanLimitResult(true, null);
    }

    public static WhatsappPlanLimitResult denied(String reason) {
        return new WhatsappPlanLimitResult(false, reason);
    }
}
