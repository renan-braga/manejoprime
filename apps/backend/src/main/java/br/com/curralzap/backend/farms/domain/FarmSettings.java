package br.com.curralzap.backend.farms.domain;

import br.com.curralzap.backend.farms.application.FarmSettingsInput;

import java.math.BigDecimal;
import java.util.UUID;

public record FarmSettings(
        UUID farmId,
        String farmName,
        BigDecimal carcassYieldPercent
) {
    public FarmSettings update(FarmSettingsInput input) {
        return new FarmSettings(
                farmId,
                input.farmName(),
                input.carcassYieldPercent()
        );
    }
}
