package br.com.curralzap.backend.whatsapp.application;

import br.com.curralzap.backend.whatsapp.domain.WhatsappAudioMedia;

import java.util.Optional;
import java.util.UUID;

public interface WhatsappAudioMediaRepository {

    WhatsappAudioMedia save(WhatsappAudioMedia audioMedia);

    Optional<WhatsappAudioMedia> findByInboundMessageId(UUID inboundMessageId);
}
