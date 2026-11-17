package br.com.curralzap.backend.whatsapp.api;

import br.com.curralzap.backend.whatsapp.application.WhatsappSupportRetryView;

public record WhatsappSupportMessageRetryAcceptedResponse(
        String status,
        String action,
        String reason
) {

    static WhatsappSupportMessageRetryAcceptedResponse from(WhatsappSupportRetryView view) {
        return new WhatsappSupportMessageRetryAcceptedResponse("accepted", view.action(), view.reason());
    }
}
