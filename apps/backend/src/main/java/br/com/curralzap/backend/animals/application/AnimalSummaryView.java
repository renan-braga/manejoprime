package br.com.curralzap.backend.animals.application;

import java.math.BigDecimal;

public record AnimalSummaryView(
        BigDecimal birthWeightKg,
        Long ageInDays,
        Long daysOnFarm
) {
}
