package br.com.curralzap.backend.farms.api;

import br.com.curralzap.backend.farms.application.FarmSummaryView;

import java.util.List;

public record MyFarmsApiResponse(List<FarmEntry> farms, int totalFarms, int maxFarms) {

    public record FarmEntry(String farmId, String farmName, String role, boolean isCurrent) {}

    public static MyFarmsApiResponse from(List<FarmSummaryView> views, int maxFarms) {
        List<FarmEntry> entries = views.stream()
                .map(v -> new FarmEntry(v.farmId().toString(), v.farmName(), v.role(), v.isCurrent()))
                .toList();
        return new MyFarmsApiResponse(entries, entries.size(), maxFarms);
    }
}
