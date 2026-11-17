package br.com.curralzap.backend.farms.api;

import br.com.curralzap.backend.farms.application.FarmSettingsInput;

record SettingsUpdateApiRequest(
        String farmName,
        String carcassYieldPercent
) {
    FarmSettingsInput toInput() {
        return FarmSettingsInput.fromApi(farmName, carcassYieldPercent);
    }
}
