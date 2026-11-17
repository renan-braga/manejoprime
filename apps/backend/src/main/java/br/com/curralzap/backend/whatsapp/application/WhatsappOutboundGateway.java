package br.com.curralzap.backend.whatsapp.application;

public interface WhatsappOutboundGateway {

    WhatsappOutboundDeliveryResult sendText(WhatsappTextOutboundMessage message);

    WhatsappOutboundDeliveryResult sendButtons(WhatsappButtonsOutboundMessage message);
}
