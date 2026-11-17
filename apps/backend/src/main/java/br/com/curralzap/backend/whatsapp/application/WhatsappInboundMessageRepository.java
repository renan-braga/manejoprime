package br.com.curralzap.backend.whatsapp.application;

import br.com.curralzap.backend.whatsapp.domain.WhatsappInboundMessage;
import br.com.curralzap.backend.whatsapp.domain.WhatsappProcessingStatus;
import br.com.curralzap.backend.whatsapp.domain.WhatsappProcessingStep;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WhatsappInboundMessageRepository {

    WhatsappInboundMessage save(WhatsappInboundMessage message);

    Optional<WhatsappInboundMessage> findById(UUID inboundMessageId);

    Optional<WhatsappInboundMessage> findByProviderMessageId(String providerMessageId);

    List<WhatsappInboundMessage> findRecentByFarmId(UUID farmId, int limit);

    List<WhatsappProcessingStep> findProcessingSteps(UUID inboundMessageId);

    WhatsappInboundMessage recordProcessingStep(
            UUID inboundMessageId,
            WhatsappProcessingStatus status,
            String details,
            OffsetDateTime occurredAt
    );
}
