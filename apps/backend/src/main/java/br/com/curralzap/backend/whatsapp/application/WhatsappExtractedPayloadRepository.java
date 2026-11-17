package br.com.curralzap.backend.whatsapp.application;

import br.com.curralzap.backend.whatsapp.domain.WhatsappExtractedPayload;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface WhatsappExtractedPayloadRepository {

    WhatsappExtractedPayload save(WhatsappExtractedPayload extractedPayload);

    Optional<WhatsappExtractedPayload> findByInboundMessageId(UUID inboundMessageId);

    long countByFarmIdAndIntentAndReceivedAtBetween(
            UUID farmId,
            String intent,
            OffsetDateTime startInclusive,
            OffsetDateTime endExclusive
    );
}
