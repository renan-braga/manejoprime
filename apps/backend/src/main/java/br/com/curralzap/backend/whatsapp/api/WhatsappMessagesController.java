package br.com.curralzap.backend.whatsapp.api;

import br.com.curralzap.backend.shared.api.ControllerSupport;
import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.whatsapp.application.WhatsappSupportMessageQuery;
import br.com.curralzap.backend.whatsapp.application.WhatsappSupportService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/whatsapp/messages")
class WhatsappMessagesController {

    private final WhatsappSupportService whatsappSupportService;

    WhatsappMessagesController(WhatsappSupportService whatsappSupportService) {
        this.whatsappSupportService = whatsappSupportService;
    }

    @GetMapping
    WhatsappSupportMessageListApiResponse listMessages(
            HttpServletRequest request,
            @RequestParam(required = false) String limit,
            @RequestParam(required = false) String offset,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status
    ) {
        AuthTokenClaims claims = requireClaims(request);
        return WhatsappSupportMessageListApiResponse.from(
                whatsappSupportService.list(claims, WhatsappSupportMessageQuery.fromApi(limit, offset, search, status))
        );
    }

    @GetMapping("/{messageId}")
    WhatsappSupportMessageApiResponse detail(
            HttpServletRequest request,
            @PathVariable UUID messageId
    ) {
        return WhatsappSupportMessageApiResponse.from(whatsappSupportService.detail(requireClaims(request), messageId));
    }

    @PostMapping("/{messageId}/retry")
    ResponseEntity<WhatsappSupportMessageRetryAcceptedResponse> retry(
            HttpServletRequest request,
            @PathVariable UUID messageId
    ) {
        return ResponseEntity.accepted().body(
                WhatsappSupportMessageRetryAcceptedResponse.from(
                        whatsappSupportService.retry(requireClaims(request), messageId)
                )
        );
    }

    private AuthTokenClaims requireClaims(HttpServletRequest request) {
        return ControllerSupport.requireClaims(request);
    }
}
