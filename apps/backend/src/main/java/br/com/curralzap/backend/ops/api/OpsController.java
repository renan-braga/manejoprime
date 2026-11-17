package br.com.curralzap.backend.ops.api;

import br.com.curralzap.backend.ops.application.OpsMonitoringService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ops")
class OpsController {

    private final OpsMonitoringService opsMonitoringService;

    OpsController(OpsMonitoringService opsMonitoringService) {
        this.opsMonitoringService = opsMonitoringService;
    }

    @GetMapping("/summary")
    OpsSummaryApiResponse summary() {
        return OpsSummaryApiResponse.from(opsMonitoringService.platformSummary());
    }

    @GetMapping("/farms")
    List<OpsFarmListApiResponse> farms() {
        return opsMonitoringService.farmList().stream()
                .map(OpsFarmListApiResponse::from)
                .toList();
    }

    @GetMapping("/farms/{farmId}")
    OpsFarmDetailApiResponse farmDetail(@PathVariable UUID farmId) {
        return OpsFarmDetailApiResponse.from(opsMonitoringService.farmDetail(farmId));
    }

    @GetMapping("/messages/stats")
    OpsMessageStatsApiResponse messageStats(@RequestParam(defaultValue = "7d") String period) {
        return OpsMessageStatsApiResponse.from(opsMonitoringService.messageStats(period));
    }

    @GetMapping("/messages/errors")
    OpsMessageErrorPageApiResponse messageErrors(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        return OpsMessageErrorPageApiResponse.from(opsMonitoringService.messageErrors(limit, offset));
    }
}
