package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.whatsapp.application.WhatsappExtractedPayloadRepository;
import br.com.curralzap.backend.whatsapp.domain.WhatsappExtractedPayload;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
class JpaWhatsappExtractedPayloadRepository implements WhatsappExtractedPayloadRepository {

    private final WhatsappExtractedPayloadJpaSpringRepository repository;

    JpaWhatsappExtractedPayloadRepository(WhatsappExtractedPayloadJpaSpringRepository repository) {
        this.repository = repository;
    }

    @Override
    public WhatsappExtractedPayload save(WhatsappExtractedPayload extractedPayload) {
        WhatsappExtractedPayloadJpaEntity entity = extractedPayload.id() == null
                ? new WhatsappExtractedPayloadJpaEntity()
                : repository.findById(extractedPayload.id()).orElseGet(WhatsappExtractedPayloadJpaEntity::new);
        entity.setInboundMessageId(extractedPayload.inboundMessageId());
        entity.setProvider(extractedPayload.provider());
        entity.setProviderModel(extractedPayload.providerModel());
        entity.setIntent(extractedPayload.intent());
        entity.setStructuredPayload(extractedPayload.structuredPayload());
        entity.setExtractedAt(extractedPayload.extractedAt());
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<WhatsappExtractedPayload> findByInboundMessageId(UUID inboundMessageId) {
        return repository.findByInboundMessageId(inboundMessageId).map(this::toDomain);
    }

    @Override
    public long countByFarmIdAndIntentAndReceivedAtBetween(
            UUID farmId,
            String intent,
            OffsetDateTime startInclusive,
            OffsetDateTime endExclusive
    ) {
        return repository.countByFarmIdAndIntentAndReceivedAtBetween(farmId, intent, startInclusive, endExclusive);
    }

    private WhatsappExtractedPayload toDomain(WhatsappExtractedPayloadJpaEntity entity) {
        return new WhatsappExtractedPayload(
                entity.getId(),
                entity.getInboundMessageId(),
                entity.getProvider(),
                entity.getProviderModel(),
                entity.getIntent(),
                entity.getStructuredPayload(),
                entity.getExtractedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
