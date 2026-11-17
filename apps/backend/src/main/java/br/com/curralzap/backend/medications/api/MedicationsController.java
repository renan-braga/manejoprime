package br.com.curralzap.backend.medications.api;

import br.com.curralzap.backend.shared.api.ControllerSupport;
import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.medications.application.MedicationListQuery;
import br.com.curralzap.backend.medications.application.MedicationService;
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
@RequestMapping("/api/v1/medications")
class MedicationsController {

    private final MedicationService medicationService;

    MedicationsController(MedicationService medicationService) {
        this.medicationService = medicationService;
    }

    @PostMapping
    MedicationApiResponse createMedication(
            HttpServletRequest request,
            @RequestBody MedicationCreateApiRequest body
    ) {
        return MedicationApiResponse.from(medicationService.create(requireClaims(request), body.toCommand()));
    }

    @GetMapping
    MedicationListApiResponse listMedications(
            HttpServletRequest request,
            @RequestParam(required = false) String limit,
            @RequestParam(required = false) String offset,
            @RequestParam(required = false) String animalId
    ) {
        return MedicationListApiResponse.from(
                medicationService.list(requireClaims(request), MedicationListQuery.fromApi(limit, offset, animalId))
        );
    }

    @DeleteMapping("/{medicationId}")
    ResponseEntity<Void> deleteMedication(
            HttpServletRequest request,
            @PathVariable UUID medicationId
    ) {
        medicationService.delete(requireClaims(request), medicationId);
        return ResponseEntity.noContent().build();
    }

    private AuthTokenClaims requireClaims(HttpServletRequest request) {
        return ControllerSupport.requireClaims(request);
    }
}
