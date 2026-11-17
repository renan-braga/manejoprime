package br.com.curralzap.backend.shared.api;

import br.com.curralzap.backend.shared.application.OperationalStatusService;
import br.com.curralzap.backend.shared.application.OperationalStatusSnapshot;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class OperationalStatusController {

    private final OperationalStatusService operationalStatusService;

    OperationalStatusController(OperationalStatusService operationalStatusService) {
        this.operationalStatusService = operationalStatusService;
    }

    @GetMapping("/internal/health")
    ResponseEntity<OperationalStatusApiResponse> health() {
        return buildResponse(operationalStatusService.health());
    }

    @GetMapping("/internal/readiness")
    ResponseEntity<OperationalStatusApiResponse> readiness() {
        return buildResponse(operationalStatusService.readiness());
    }

    private ResponseEntity<OperationalStatusApiResponse> buildResponse(OperationalStatusSnapshot snapshot) {
        HttpStatus httpStatus = snapshot.up() ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(httpStatus).body(OperationalStatusApiResponse.from(snapshot));
    }
}
