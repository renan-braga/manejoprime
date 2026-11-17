package br.com.curralzap.backend.whatsapp.api;

import br.com.curralzap.backend.shared.api.RequestIdFilter;
import br.com.curralzap.backend.whatsapp.application.InvalidWhatsappWebhookAuthenticationException;
import br.com.curralzap.backend.whatsapp.application.WhatsappWebhookIntakeService;
import br.com.curralzap.backend.whatsapp.application.WhatsappWebhookPayloadParser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/whatsapp/webhooks")
class WhatsappWebhookController {

    private final String configuredSecret;
    private final WhatsappWebhookPayloadParser payloadParser;
    private final WhatsappWebhookIntakeService intakeService;

    WhatsappWebhookController(
            @Value("${curralzap.whatsapp.webhook.secret}") String configuredSecret,
            WhatsappWebhookPayloadParser payloadParser,
            WhatsappWebhookIntakeService intakeService
    ) {
        this.configuredSecret = configuredSecret;
        this.payloadParser = payloadParser;
        this.intakeService = intakeService;
    }

    @PostMapping("/evolution")
    ResponseEntity<WhatsappWebhookAcceptedResponse> receiveEvolutionWebhook(
            @RequestHeader(value = "X-Webhook-Secret", required = false) String providedSecret,
            @RequestBody(required = false) String rawPayload,
            HttpServletRequest request
    ) {
        if (providedSecret == null || providedSecret.isBlank() || !configuredSecret.equals(providedSecret)) {
            throw new InvalidWhatsappWebhookAuthenticationException();
        }

        String requestId = (String) request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
        intakeService.intake(payloadParser.parse(rawPayload), requestId);
        return ResponseEntity.accepted().body(new WhatsappWebhookAcceptedResponse("accepted"));
    }
}
