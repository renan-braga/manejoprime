package br.com.curralzap.backend.reports.api;

import br.com.curralzap.backend.shared.api.ControllerSupport;
import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.reports.application.DashboardService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
class DashboardController {

    private final DashboardService dashboardService;

    DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    DashboardApiResponse dashboard(HttpServletRequest request) {
        AuthTokenClaims claims = requireClaims(request);
        return DashboardApiResponse.from(dashboardService.load(claims));
    }

    @GetMapping("/weight-trend")
    List<WeightTrendApiResponse> weightTrend(HttpServletRequest request) {
        AuthTokenClaims claims = requireClaims(request);
        return dashboardService.loadWeightTrend(claims).stream()
                .map(WeightTrendApiResponse::from)
                .toList();
    }

    @GetMapping("/lot-breakdown")
    List<LotBreakdownApiResponse> lotBreakdown(HttpServletRequest request) {
        AuthTokenClaims claims = requireClaims(request);
        return dashboardService.loadLotBreakdown(claims).stream()
                .map(LotBreakdownApiResponse::from)
                .toList();
    }

    private AuthTokenClaims requireClaims(HttpServletRequest request) {
        return ControllerSupport.requireClaims(request);
    }
}
