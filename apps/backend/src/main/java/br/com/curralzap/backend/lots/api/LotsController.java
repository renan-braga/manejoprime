package br.com.curralzap.backend.lots.api;

import br.com.curralzap.backend.shared.api.ControllerSupport;
import br.com.curralzap.backend.shared.api.ImportResultApiResponse;
import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.lots.application.LotImportService;
import br.com.curralzap.backend.lots.application.LotListQuery;
import br.com.curralzap.backend.lots.application.LotService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/lots")
class LotsController {

    private final LotService lotService;
    private final LotImportService lotImportService;

    LotsController(LotService lotService, LotImportService lotImportService) {
        this.lotService = lotService;
        this.lotImportService = lotImportService;
    }

    @GetMapping
    LotListApiResponse listLots(
            HttpServletRequest request,
            @RequestParam(required = false) String limit,
            @RequestParam(required = false) String offset,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String purpose
    ) {
        return LotListApiResponse.from(
                lotService.list(requireClaims(request), LotListQuery.fromApi(limit, offset, search, purpose))
        );
    }

    @GetMapping("/{lotId}")
    LotApiResponse detailLot(
            HttpServletRequest request,
            @PathVariable UUID lotId
    ) {
        return LotApiResponse.from(lotService.detail(requireClaims(request), lotId));
    }

    @PostMapping
    LotApiResponse createLot(
            HttpServletRequest request,
            @RequestBody LotUpsertApiRequest body
    ) {
        return LotApiResponse.from(lotService.create(requireClaims(request), body.toInput()));
    }

    @PatchMapping("/{lotId}")
    LotApiResponse updateLot(
            HttpServletRequest request,
            @PathVariable UUID lotId,
            @RequestBody LotUpsertApiRequest body
    ) {
        return LotApiResponse.from(lotService.update(requireClaims(request), lotId, body.toInput()));
    }

    @PostMapping("/import")
    ImportResultApiResponse importLots(
            HttpServletRequest request,
            @RequestBody LotImportApiRequest body
    ) {
        return ImportResultApiResponse.from(lotImportService.importLots(requireClaims(request), body.rows()));
    }

    private AuthTokenClaims requireClaims(HttpServletRequest request) {
        return ControllerSupport.requireClaims(request);
    }
}
