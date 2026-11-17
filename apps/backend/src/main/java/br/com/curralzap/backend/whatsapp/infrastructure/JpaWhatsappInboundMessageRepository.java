package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.whatsapp.application.WhatsappInboundMessageRepository;
import br.com.curralzap.backend.whatsapp.domain.WhatsappInboundMessage;
import br.com.curralzap.backend.whatsapp.domain.WhatsappProcessingStatus;
import br.com.curralzap.backend.whatsapp.domain.WhatsappProcessingStep;
import org.springframework.data.domain.PageRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
class JpaWhatsappInboundMessageRepository implements WhatsappInboundMessageRepository {

    private final WhatsappInboundMessageJpaSpringRepository messageRepository;
    private final WhatsappProcessingStepJpaSpringRepository processingStepRepository;
    private final TransactionTemplate transactionTemplate;

    JpaWhatsappInboundMessageRepository(
            WhatsappInboundMessageJpaSpringRepository messageRepository,
            WhatsappProcessingStepJpaSpringRepository processingStepRepository,
            PlatformTransactionManager transactionManager
    ) {
        this.messageRepository = messageRepository;
        this.processingStepRepository = processingStepRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public WhatsappInboundMessage save(WhatsappInboundMessage message) {
        boolean newMessage = message.id() == null;
        if (!newMessage) {
            return Objects.requireNonNull(transactionTemplate.execute(status -> saveExistingMessage(message)));
        }

        Optional<WhatsappInboundMessage> existing = findByProviderMessageId(message.providerMessageId());
        if (existing.isPresent()) {
            return existing.get();
        }

        try {
            return Objects.requireNonNull(transactionTemplate.execute(status -> saveNewMessage(message)));
        } catch (DataIntegrityViolationException exception) {
            return findByProviderMessageId(message.providerMessageId())
                    .orElseThrow(() -> exception);
        }
    }

    @Override
    public Optional<WhatsappInboundMessage> findByProviderMessageId(String providerMessageId) {
        return messageRepository.findByProviderMessageId(providerMessageId).map(this::toDomain);
    }

    @Override
    public Optional<WhatsappInboundMessage> findById(UUID inboundMessageId) {
        return messageRepository.findById(inboundMessageId).map(this::toDomain);
    }

    @Override
    public List<WhatsappInboundMessage> findRecentByFarmId(UUID farmId, int limit) {
        return messageRepository.findByFarmIdOrderByCreatedAtDesc(farmId, PageRequest.of(0, limit)).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<WhatsappProcessingStep> findProcessingSteps(UUID inboundMessageId) {
        return processingStepRepository.findByInboundMessageIdOrderByOccurredAtAsc(inboundMessageId)
                .stream()
                .map(this::toProcessingStep)
                .toList();
    }

    @Override
    @Transactional
    public WhatsappInboundMessage recordProcessingStep(
            UUID inboundMessageId,
            WhatsappProcessingStatus status,
            String details,
            OffsetDateTime occurredAt
    ) {
        WhatsappInboundMessageJpaEntity entity = messageRepository.findById(inboundMessageId)
                .orElseThrow(() -> new IllegalArgumentException("Inbound message not found: " + inboundMessageId));

        entity.setStatus(status);
        entity.setFailureReason(status == WhatsappProcessingStatus.FAILED ? details : null);
        processingStepRepository.save(newProcessingStep(inboundMessageId, status, details, occurredAt));

        return toDomain(messageRepository.save(entity));
    }

    private WhatsappInboundMessage saveExistingMessage(WhatsappInboundMessage message) {
        WhatsappInboundMessageJpaEntity entity = messageRepository.findById(message.id())
                .orElseGet(WhatsappInboundMessageJpaEntity::new);
        copyToEntity(message, entity);
        return toDomain(messageRepository.save(entity));
    }

    private WhatsappInboundMessage saveNewMessage(WhatsappInboundMessage message) {
        WhatsappInboundMessageJpaEntity entity = new WhatsappInboundMessageJpaEntity();
        copyToEntity(message, entity);

        WhatsappInboundMessageJpaEntity savedEntity = messageRepository.saveAndFlush(entity);
        processingStepRepository.save(newProcessingStep(
                savedEntity.getId(),
                savedEntity.getStatus(),
                null,
                savedEntity.getReceivedAt()
        ));

        return toDomain(savedEntity);
    }

    private void copyToEntity(WhatsappInboundMessage message, WhatsappInboundMessageJpaEntity entity) {
        entity.setProviderMessageId(message.providerMessageId());
        entity.setEventType(message.eventType());
        entity.setPhoneNumber(message.phoneNumber());
        entity.setFarmIdValue(message.farmId());
        entity.setUserId(message.userId());
        entity.setCorrelationId(message.correlationId());
        entity.setMessageType(message.messageType());
        entity.setRawPayload(message.rawPayload());
        entity.setStatus(message.status());
        entity.setFailureReason(message.failureReason());
        entity.setReceivedAt(message.receivedAt());
    }

    private WhatsappInboundMessage toDomain(WhatsappInboundMessageJpaEntity entity) {
        return new WhatsappInboundMessage(
                entity.getId(),
                entity.getProviderMessageId(),
                entity.getEventType(),
                entity.getPhoneNumber(),
                entity.getFarmIdValue(),
                entity.getUserId(),
                entity.getCorrelationId(),
                entity.getMessageType(),
                entity.getRawPayload(),
                entity.getStatus(),
                entity.getFailureReason(),
                entity.getReceivedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private WhatsappProcessingStep toProcessingStep(WhatsappProcessingStepJpaEntity entity) {
        return new WhatsappProcessingStep(
                entity.getId(),
                entity.getInboundMessageId(),
                entity.getStatus(),
                entity.getDetails(),
                entity.getOccurredAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private WhatsappProcessingStepJpaEntity newProcessingStep(
            UUID inboundMessageId,
            WhatsappProcessingStatus status,
            String details,
            OffsetDateTime occurredAt
    ) {
        WhatsappProcessingStepJpaEntity entity = new WhatsappProcessingStepJpaEntity();
        entity.setInboundMessageId(inboundMessageId);
        entity.setStatus(status);
        entity.setDetails(details);
        entity.setOccurredAt(occurredAt);
        return entity;
    }
}
