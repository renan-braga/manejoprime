package br.com.curralzap.backend.occurrences.api;

import br.com.curralzap.backend.shared.api.ControllerSupport;
import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.occurrences.application.OccurrenceActionService;
import br.com.curralzap.backend.occurrences.application.OccurrenceListQuery;
import br.com.curralzap.backend.occurrences.application.OccurrenceQueryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/occurrences")
class OccurrencesController {

    private final OccurrenceActionService occurrenceActionService;
    private final OccurrenceQueryService occurrenceQueryService;

    OccurrencesController(
            OccurrenceActionService occurrenceActionService,
            OccurrenceQueryService occurrenceQueryService
    ) {
        this.occurrenceActionService = occurrenceActionService;
        this.occurrenceQueryService = occurrenceQueryService;
    }

    @GetMapping
    OccurrenceListApiResponse listOccurrences(
            HttpServletRequest request,
            @RequestParam(required = false) String limit,
            @RequestParam(required = false) String offset,
            @RequestParam(required = false) String animalId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity
    ) {
        return OccurrenceListApiResponse.from(
                occurrenceQueryService.list(
                        requireClaims(request),
                        OccurrenceListQuery.fromApi(limit, offset, animalId, status, severity)
                )
        );
    }

    @PostMapping
    OccurrenceApiResponse createOccurrence(
            HttpServletRequest request,
            @RequestBody OccurrenceCreateApiRequest body
    ) {
        return OccurrenceApiResponse.from(
                occurrenceActionService.register(requireClaims(request), body.toCommand())
        );
    }

    @PostMapping("/{occurrenceId}/resolve")
    OccurrenceActionApiResponse resolveOccurrence(
            HttpServletRequest request,
            @PathVariable UUID occurrenceId,
            @RequestBody(required = false) OccurrenceResolveApiRequest body
    ) {
        return OccurrenceActionApiResponse.from(
                occurrenceActionService.resolve(requireClaims(request), occurrenceId)
        );
    }

    @PostMapping("/{occurrenceId}/reopen")
    OccurrenceActionApiResponse reopenOccurrence(
            HttpServletRequest request,
            @PathVariable UUID occurrenceId
    ) {
        return OccurrenceActionApiResponse.from(
                occurrenceActionService.reopen(requireClaims(request), occurrenceId)
        );
    }

    @DeleteMapping("/{occurrenceId}")
    ResponseEntity<Void> deleteOccurrence(
            HttpServletRequest request,
            @PathVariable UUID occurrenceId
    ) {
        occurrenceActionService.delete(requireClaims(request), occurrenceId);
        return ResponseEntity.noContent().build();
    }

    private AuthTokenClaims requireClaims(HttpServletRequest request) {
        return ControllerSupport.requireClaims(request);
    }
}
