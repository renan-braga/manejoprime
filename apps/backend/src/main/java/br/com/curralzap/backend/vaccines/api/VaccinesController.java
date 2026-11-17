package br.com.curralzap.backend.vaccines.api;

import br.com.curralzap.backend.shared.api.ControllerSupport;
import br.com.curralzap.backend.shared.api.ImportResultApiResponse;
import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.vaccines.application.VaccineImportService;
import br.com.curralzap.backend.vaccines.application.VaccineListQuery;
import br.com.curralzap.backend.vaccines.application.VaccineService;
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
@RequestMapping("/api/v1/vaccines")
class VaccinesController {

    private final VaccineService vaccineService;
    private final VaccineImportService vaccineImportService;

    VaccinesController(VaccineService vaccineService, VaccineImportService vaccineImportService) {
        this.vaccineService = vaccineService;
        this.vaccineImportService = vaccineImportService;
    }

    @PostMapping
    VaccineApiResponse createVaccine(
            HttpServletRequest request,
            @RequestBody VaccineCreateApiRequest body
    ) {
        return VaccineApiResponse.from(vaccineService.create(requireClaims(request), body.toCommand()));
    }

    @GetMapping
    VaccineListApiResponse listVaccines(
            HttpServletRequest request,
            @RequestParam(required = false) String limit,
            @RequestParam(required = false) String offset,
            @RequestParam(required = false) String animalId,
            @RequestParam(required = false) String lotId
    ) {
        return VaccineListApiResponse.from(
                vaccineService.list(requireClaims(request), VaccineListQuery.fromApi(limit, offset, animalId, lotId))
        );
    }

    @DeleteMapping("/{vaccineId}")
    ResponseEntity<Void> deleteVaccine(
            HttpServletRequest request,
            @PathVariable UUID vaccineId
    ) {
        vaccineService.delete(requireClaims(request), vaccineId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/import")
    ImportResultApiResponse importVaccines(
            HttpServletRequest request,
            @RequestBody VaccineImportApiRequest body
    ) {
        return ImportResultApiResponse.from(vaccineImportService.importVaccines(requireClaims(request), body.rows()));
    }

    private AuthTokenClaims requireClaims(HttpServletRequest request) {
        return ControllerSupport.requireClaims(request);
    }
}
