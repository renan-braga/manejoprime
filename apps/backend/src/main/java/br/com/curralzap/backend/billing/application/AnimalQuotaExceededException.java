package br.com.curralzap.backend.billing.application;

public class AnimalQuotaExceededException extends RuntimeException {

    private final long currentCount;
    private final int planLimit;
    private final int importCount;
    private final String plan;

    public AnimalQuotaExceededException(String plan, long currentCount, int planLimit, int importCount) {
        super(String.format(
                "Seu plano (%s) permite até %d animais. Você tem %d e está tentando importar %d.",
                plan, planLimit, currentCount, importCount
        ));
        this.plan = plan;
        this.currentCount = currentCount;
        this.planLimit = planLimit;
        this.importCount = importCount;
    }

    public long currentCount() { return currentCount; }
    public int planLimit() { return planLimit; }
    public int importCount() { return importCount; }
    public String plan() { return plan; }
}
