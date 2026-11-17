package br.com.curralzap.backend.billing.api;

import br.com.curralzap.backend.billing.application.AsaasWebhookProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/billing/webhook")
class AsaasWebhookController {

    private final AsaasWebhookProcessor processor;
    private final String webhookToken;

    AsaasWebhookController(
            AsaasWebhookProcessor processor,
            @Value("${curralzap.asaas.webhook-token:}") String webhookToken
    ) {
        this.processor = processor;
        this.webhookToken = webhookToken;
    }

    @PostMapping("/asaas")
    ResponseEntity<Void> receive(
            @RequestHeader(value = "asaas-access-token", required = false) String token,
            @RequestBody String rawPayload
    ) {
        if (webhookToken.isBlank() || !webhookToken.equals(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        processor.process(rawPayload);
        return ResponseEntity.ok().build();
    }
}
