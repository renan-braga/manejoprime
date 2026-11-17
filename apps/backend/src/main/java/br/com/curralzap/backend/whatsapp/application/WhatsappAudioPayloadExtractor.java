package br.com.curralzap.backend.whatsapp.application;

import br.com.curralzap.backend.whatsapp.domain.WhatsappInboundMessage;

import java.util.Optional;

public interface WhatsappAudioPayloadExtractor {

    Optional<WhatsappAudioPayload> extract(WhatsappInboundMessage message);
}
