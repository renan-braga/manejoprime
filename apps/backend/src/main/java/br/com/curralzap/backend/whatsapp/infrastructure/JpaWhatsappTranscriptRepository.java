package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.whatsapp.application.WhatsappTranscriptRepository;
import br.com.curralzap.backend.whatsapp.domain.WhatsappTranscript;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
class JpaWhatsappTranscriptRepository implements WhatsappTranscriptRepository {

    private final WhatsappTranscriptJpaSpringRepository repository;

    JpaWhatsappTranscriptRepository(WhatsappTranscriptJpaSpringRepository repository) {
        this.repository = repository;
    }

    @Override
    public WhatsappTranscript save(WhatsappTranscript transcript) {
        WhatsappTranscriptJpaEntity entity = transcript.id() == null
                ? new WhatsappTranscriptJpaEntity()
                : repository.findById(transcript.id()).orElseGet(WhatsappTranscriptJpaEntity::new);
        entity.setInboundMessageId(transcript.inboundMessageId());
        entity.setProvider(transcript.provider());
        entity.setProviderModel(transcript.providerModel());
        entity.setTranscript(transcript.transcript());
        entity.setTranscribedAt(transcript.transcribedAt());
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<WhatsappTranscript> findByInboundMessageId(UUID inboundMessageId) {
        return repository.findByInboundMessageId(inboundMessageId).map(this::toDomain);
    }

    private WhatsappTranscript toDomain(WhatsappTranscriptJpaEntity entity) {
        return new WhatsappTranscript(
                entity.getId(),
                entity.getInboundMessageId(),
                entity.getProvider(),
                entity.getProviderModel(),
                entity.getTranscript(),
                entity.getTranscribedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
