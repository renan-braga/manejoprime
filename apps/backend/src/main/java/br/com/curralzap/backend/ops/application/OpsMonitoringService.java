package br.com.curralzap.backend.ops.application;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class OpsMonitoringService {

    private static final Set<String> VALID_PERIODS = Set.of("24h", "7d", "30d");

    private final OpsViewRepository opsViewRepository;

    public OpsMonitoringService(OpsViewRepository opsViewRepository) {
        this.opsViewRepository = opsViewRepository;
    }

    public OpsPlatformSummaryView platformSummary() {
        return opsViewRepository.loadPlatformSummary();
    }

    public List<OpsFarmListItemView> farmList() {
        return opsViewRepository.loadFarmList();
    }

    public OpsFarmDetailView farmDetail(UUID farmId) {
        return opsViewRepository.loadFarmDetail(farmId);
    }

    public OpsMessageStatsView messageStats(String period) {
        if (!VALID_PERIODS.contains(period)) {
            throw new InvalidOpsPeriodException(period);
        }
        return opsViewRepository.loadMessageStats(period);
    }

    public OpsMessageErrorPageView messageErrors(int limit, int offset) {
        return opsViewRepository.loadMessageErrors(limit, offset);
    }
}
