package br.com.curralzap.backend.whatsapp.application;

import br.com.curralzap.backend.whatsapp.domain.WhatsappTranscript;

import java.util.Optional;
import java.util.UUID;

public interface WhatsappTranscriptRepository {

    WhatsappTranscript save(WhatsappTranscript transcript);

    Optional<WhatsappTranscript> findByInboundMessageId(UUID inboundMessageId);
}
