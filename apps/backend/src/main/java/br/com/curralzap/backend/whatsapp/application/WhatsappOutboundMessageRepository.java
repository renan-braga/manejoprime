package br.com.curralzap.backend.whatsapp.application;

import br.com.curralzap.backend.whatsapp.domain.WhatsappOutboundMessage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WhatsappOutboundMessageRepository {

    WhatsappOutboundMessage save(WhatsappOutboundMessage outboundMessage);

    Optional<WhatsappOutboundMessage> findById(UUID outboundMessageId);

    Optional<WhatsappOutboundMessage> findByProviderMessageId(String providerMessageId);

    List<WhatsappOutboundMessage> findByInboundMessageIdOrderByCreatedAtAsc(UUID inboundMessageId);
}
