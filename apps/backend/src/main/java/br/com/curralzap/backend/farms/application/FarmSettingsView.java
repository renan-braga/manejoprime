package br.com.curralzap.backend.farms.application;

import java.math.BigDecimal;
import java.util.UUID;

public record FarmSettingsView(
        UUID farmId,
        String farmName,
        BigDecimal carcassYieldPercent
) {
}
