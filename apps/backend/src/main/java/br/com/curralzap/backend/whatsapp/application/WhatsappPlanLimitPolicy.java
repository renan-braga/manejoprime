package br.com.curralzap.backend.whatsapp.application;

public interface WhatsappPlanLimitPolicy {

    WhatsappPlanLimitResult check(WhatsappPlanLimitRequest request);
}
