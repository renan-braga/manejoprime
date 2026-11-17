package br.com.curralzap.backend.ops.application;

import java.util.List;
import java.util.UUID;

public interface OpsViewRepository {
    OpsPlatformSummaryView loadPlatformSummary();
    List<OpsFarmListItemView> loadFarmList();
    OpsFarmDetailView loadFarmDetail(UUID farmId);
    OpsMessageStatsView loadMessageStats(String period);
    OpsMessageErrorPageView loadMessageErrors(int limit, int offset);
}
