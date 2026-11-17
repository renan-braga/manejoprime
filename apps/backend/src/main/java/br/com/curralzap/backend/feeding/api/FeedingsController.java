package br.com.curralzap.backend.feeding.api;

import br.com.curralzap.backend.shared.api.ControllerSupport;
import br.com.curralzap.backend.shared.api.ImportResultApiResponse;
import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.feeding.application.FeedingImportService;
import br.com.curralzap.backend.feeding.application.FeedingListQuery;
import br.com.curralzap.backend.feeding.application.FeedingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/feedings")
class FeedingsController {

    private final FeedingService feedingService;
    private final FeedingImportService feedingImportService;

    FeedingsController(FeedingService feedingService, FeedingImportService feedingImportService) {
        this.feedingService = feedingService;
        this.feedingImportService = feedingImportService;
    }

    @PostMapping
    FeedingApiResponse createFeeding(
            HttpServletRequest request,
            @RequestBody FeedingCreateApiRequest body
    ) {
        return FeedingApiResponse.from(feedingService.create(requireClaims(request), body.toCommand()));
    }

    @PostMapping("/import")
    ImportResultApiResponse importFeedings(
            HttpServletRequest request,
            @RequestBody FeedingImportApiRequest body
    ) {
        return ImportResultApiResponse.from(feedingImportService.importFeedings(requireClaims(request), body.rows()));
    }

    @GetMapping
    FeedingListApiResponse listFeedings(
            HttpServletRequest request,
            @RequestParam(required = false) String limit,
            @RequestParam(required = false) String offset,
            @RequestParam(required = false) String animalId,
            @RequestParam(required = false) String lotId
    ) {
        return FeedingListApiResponse.from(
                feedingService.list(requireClaims(request), FeedingListQuery.fromApi(limit, offset, animalId, lotId))
        );
    }

    private AuthTokenClaims requireClaims(HttpServletRequest request) {
        return ControllerSupport.requireClaims(request);
    }
}
