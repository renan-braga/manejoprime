package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.whatsapp.application.WhatsappOutboundMessageRepository;
import br.com.curralzap.backend.whatsapp.domain.WhatsappOutboundMessage;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class JpaWhatsappOutboundMessageRepository implements WhatsappOutboundMessageRepository {

    private final WhatsappOutboundMessageJpaSpringRepository repository;

    JpaWhatsappOutboundMessageRepository(WhatsappOutboundMessageJpaSpringRepository repository) {
        this.repository = repository;
    }

    @Override
    public WhatsappOutboundMessage save(WhatsappOutboundMessage outboundMessage) {
        WhatsappOutboundMessageJpaEntity entity = outboundMessage.id() == null
                ? new WhatsappOutboundMessageJpaEntity()
                : repository.findById(outboundMessage.id()).orElseGet(WhatsappOutboundMessageJpaEntity::new);
        entity.setInboundMessageId(outboundMessage.inboundMessageId());
        entity.setFarmId(outboundMessage.farmId());
        entity.setUserId(outboundMessage.userId());
        entity.setCorrelationId(outboundMessage.correlationId());
        entity.setPhoneNumber(outboundMessage.phoneNumber());
        entity.setMessageType(outboundMessage.messageType());
        entity.setPayload(outboundMessage.payload());
        entity.setProviderMessageId(outboundMessage.providerMessageId());
        entity.setStatus(outboundMessage.status());
        entity.setFailureReason(outboundMessage.failureReason());
        entity.setQueuedAt(outboundMessage.queuedAt());
        entity.setSentAt(outboundMessage.sentAt());
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<WhatsappOutboundMessage> findById(UUID outboundMessageId) {
        return repository.findById(outboundMessageId).map(this::toDomain);
    }

    @Override
    public Optional<WhatsappOutboundMessage> findByProviderMessageId(String providerMessageId) {
        return repository.findByProviderMessageId(providerMessageId).map(this::toDomain);
    }

    @Override
    public List<WhatsappOutboundMessage> findByInboundMessageIdOrderByCreatedAtAsc(UUID inboundMessageId) {
        return repository.findByInboundMessageIdOrderByCreatedAtAsc(inboundMessageId).stream()
                .map(this::toDomain)
                .toList();
    }

    private WhatsappOutboundMessage toDomain(WhatsappOutboundMessageJpaEntity entity) {
        return new WhatsappOutboundMessage(
                entity.getId(),
                entity.getInboundMessageId(),
                entity.getFarmId(),
                entity.getUserId(),
                entity.getCorrelationId(),
                entity.getPhoneNumber(),
                entity.getMessageType(),
                entity.getPayload(),
                entity.getProviderMessageId(),
                entity.getStatus(),
                entity.getFailureReason(),
                entity.getQueuedAt(),
                entity.getSentAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
