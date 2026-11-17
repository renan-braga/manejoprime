package br.com.curralzap.backend.reports.application;

import java.util.List;
import java.util.UUID;

public interface DashboardViewRepository {

    DashboardAggregateView loadFor(UUID farmId);

    List<WeightTrendItemView> loadWeightTrend(UUID farmId);

    List<LotBreakdownItemView> loadLotBreakdown(UUID farmId);
}
