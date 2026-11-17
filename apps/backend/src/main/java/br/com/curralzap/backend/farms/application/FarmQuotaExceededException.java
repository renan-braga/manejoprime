package br.com.curralzap.backend.farms.application;

public class FarmQuotaExceededException extends RuntimeException {

    private final int limit;

    public FarmQuotaExceededException(int limit) {
        super("Farm quota exceeded. Plan allows " + limit + " farm(s).");
        this.limit = limit;
    }

    public int getLimit() {
        return limit;
    }
}
