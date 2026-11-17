package br.com.curralzap.backend.weighings.api;

import br.com.curralzap.backend.shared.api.ControllerSupport;
import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.weighings.application.WeighingListQuery;
import br.com.curralzap.backend.weighings.application.WeighingQueryService;
import br.com.curralzap.backend.weighings.application.WeighingWriteService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/weighings")
class WeighingsController {

    private final WeighingWriteService weighingWriteService;
    private final WeighingQueryService weighingQueryService;

    WeighingsController(WeighingWriteService weighingWriteService, WeighingQueryService weighingQueryService) {
        this.weighingWriteService = weighingWriteService;
        this.weighingQueryService = weighingQueryService;
    }

    @PostMapping
    WeighingCreateApiResponse createWeighing(
            HttpServletRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody WeighingCreateApiRequest body
    ) {
        return WeighingCreateApiResponse.from(
                weighingWriteService.create(requireClaims(request), body.toCommand(), normalizeIdempotencyKey(idempotencyKey))
        );
    }

    @GetMapping
    WeighingHistoryApiResponse listWeighings(
            HttpServletRequest request,
            @RequestParam(required = false) String limit,
            @RequestParam(required = false) String offset,
            @RequestParam(required = false) String animalId
    ) {
        return WeighingHistoryApiResponse.from(
                weighingQueryService.list(requireClaims(request), WeighingListQuery.fromApi(limit, offset, animalId))
        );
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        return idempotencyKey.trim();
    }

    private AuthTokenClaims requireClaims(HttpServletRequest request) {
        return ControllerSupport.requireClaims(request);
    }
}
