package br.com.curralzap.backend.billing.application;

import java.time.OffsetDateTime;
import java.util.Map;

public class BillingAccessDeniedException extends RuntimeException {

    private final BillingSnapshot snapshot;

    public BillingAccessDeniedException(BillingSnapshot snapshot) {
        super("Seu plano esta suspenso ou exige pagamento. Regularize a cobranca para continuar usando esta acao.");
        this.snapshot = snapshot;
    }

    public BillingSnapshot snapshot() {
        return snapshot;
    }

    public Map<String, Object> details() {
        OffsetDateTime graceUntil = snapshot.graceUntil();
        return Map.of(
                "plan", snapshot.plan(),
                "status", snapshot.status(),
                "billingExempt", snapshot.billingExempt(),
                "canWrite", snapshot.canWrite(),
                "canUseWhatsapp", snapshot.canUseWhatsapp(),
                "graceUntil", graceUntil != null ? graceUntil.toString() : ""
        );
    }
}
