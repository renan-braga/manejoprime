package br.com.curralzap.backend.testing.api;

import br.com.curralzap.backend.testing.application.TestingSeedResult;
import br.com.curralzap.backend.testing.application.TestingSeedService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/testing")
@ConditionalOnProperty("curralzap.testing.seed.enabled")
class TestingController {

    private final TestingSeedService seedService;
    private final String seedSecret;

    TestingController(
            TestingSeedService seedService,
            @org.springframework.beans.factory.annotation.Value("${curralzap.testing.seed.secret:}") String seedSecret
    ) {
        this.seedService = seedService;
        this.seedSecret = seedSecret;
    }

    @PostMapping("/seed")
    ResponseEntity<TestingSeedApiResponse> seed(HttpServletRequest request) {
        requireSeedSecret(request);
        TestingSeedResult result = seedService.seed();
        return ResponseEntity.ok(TestingSeedApiResponse.from(result));
    }

    @DeleteMapping("/seed/{farmId}")
    ResponseEntity<Void> teardown(@PathVariable UUID farmId, HttpServletRequest request) {
        requireSeedSecret(request);
        seedService.teardown(farmId);
        return ResponseEntity.noContent().build();
    }

    private void requireSeedSecret(HttpServletRequest request) {
        String provided = request.getHeader("X-Test-Seed-Secret");
        if (seedSecret.isBlank() || !seedSecret.equals(provided)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid test seed secret.");
        }
    }
}
