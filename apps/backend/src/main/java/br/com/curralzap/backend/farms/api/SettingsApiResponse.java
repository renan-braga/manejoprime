package br.com.curralzap.backend.farms.api;

import br.com.curralzap.backend.farms.application.FarmSettingsView;

import java.math.RoundingMode;
import java.util.UUID;

record SettingsApiResponse(
        UUID farmId,
        String farmName,
        String carcassYieldPercent
) {
    static SettingsApiResponse from(FarmSettingsView settings) {
        return new SettingsApiResponse(
                settings.farmId(),
                settings.farmName(),
                settings.carcassYieldPercent().setScale(2, RoundingMode.UNNECESSARY).toPlainString()
        );
    }
}
