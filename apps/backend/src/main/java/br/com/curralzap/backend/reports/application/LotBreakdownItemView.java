package br.com.curralzap.backend.reports.application;

import java.math.BigDecimal;
import java.util.UUID;

public record LotBreakdownItemView(UUID lotId, String lotName, long headCount, BigDecimal avgWeight) {
}
