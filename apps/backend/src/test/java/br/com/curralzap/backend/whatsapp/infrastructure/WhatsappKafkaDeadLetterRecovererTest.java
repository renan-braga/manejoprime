package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.whatsapp.application.WhatsappInboundMessageRepository;
import br.com.curralzap.backend.whatsapp.application.WhatsappMessageReceivedEvent;
import br.com.curralzap.backend.whatsapp.domain.WhatsappInboundMessage;
import br.com.curralzap.backend.whatsapp.domain.WhatsappProcessingStatus;
import br.com.curralzap.backend.whatsapp.domain.WhatsappProcessingStep;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WhatsappKafkaDeadLetterRecovererTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-04-29T18:45:00Z"), ZoneOffset.UTC);

    @Test
    void publishesOriginalEventToDeadLetterTopicAndRecordsSupportFailure() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<Object, Object> kafkaTemplate = mock(KafkaTemplate.class);
        InMemoryWhatsappInboundMessageRepository inboundMessageRepository = new InMemoryWhatsappInboundMessageRepository();
        WhatsappInboundMessage inboundMessage = inboundMessageRepository.save(WhatsappInboundMessage.receive(
                "wamid.dlt-1",
                "messages.upsert",
                "5511999990011",
                null,
                null,
                "corr-dlt-1",
                "audioMessage",
                "{\"event\":\"messages.upsert\"}",
                OffsetDateTime.parse("2026-04-29T18:40:00Z")
        ));
        WhatsappMessageReceivedEvent event = new WhatsappMessageReceivedEvent(
                1,
                "whatsapp.message.received.v1",
                inboundMessage.id().toString(),
                "corr-dlt-1",
                inboundMessage.receivedAt(),
                inboundMessage.id(),
                inboundMessage.providerMessageId(),
                inboundMessage.farmId(),
                inboundMessage.userId(),
                inboundMessage.phoneNumber(),
                inboundMessage.messageType(),
                inboundMessage.status()
        );
        WhatsappKafkaDeadLetterRecoverer recoverer = new WhatsappKafkaDeadLetterRecoverer(
                kafkaTemplate,
                inboundMessageRepository,
                Map.of("whatsapp.message.received", "whatsapp.message.received.dlt"),
                FIXED_CLOCK
        );

        recoverer.accept(
                new ConsumerRecord<>("whatsapp.message.received", 0, 0L, "wamid.dlt-1", event),
                new IllegalStateException("Retry attempts exhausted.")
        );

        verify(kafkaTemplate).send("whatsapp.message.received.dlt", "wamid.dlt-1", event);
        assertThat(inboundMessageRepository.findByProviderMessageId("wamid.dlt-1"))
                .get()
                .extracting(WhatsappInboundMessage::status)
                .isEqualTo(WhatsappProcessingStatus.FAILED);
        assertThat(inboundMessageRepository.findProcessingSteps(inboundMessage.id()))
                .last()
                .satisfies(step -> {
                    assertThat(step.status()).isEqualTo(WhatsappProcessingStatus.FAILED);
                    assertThat(step.details())
                            .isEqualTo("Worker dead-lettered from whatsapp.message.received: Retry attempts exhausted.");
                });
    }

    private static final class InMemoryWhatsappInboundMessageRepository implements WhatsappInboundMessageRepository {

        private final Map<UUID, WhatsappInboundMessage> messagesById = new LinkedHashMap<>();
        private final Map<UUID, List<WhatsappProcessingStep>> stepsByMessageId = new LinkedHashMap<>();

        @Override
        public WhatsappInboundMessage save(WhatsappInboundMessage message) {
            WhatsappInboundMessage persisted = message.id() == null
                    ? new WhatsappInboundMessage(
                    UUID.randomUUID(),
                    message.providerMessageId(),
                    message.eventType(),
                    message.phoneNumber(),
                    message.farmId(),
                    message.userId(),
                    message.correlationId(),
                    message.messageType(),
                    message.rawPayload(),
                    message.status(),
                    message.failureReason(),
                    message.receivedAt(),
                    message.createdAt(),
                    message.updatedAt()
            )
                    : message;
            messagesById.put(persisted.id(), persisted);
            stepsByMessageId.putIfAbsent(persisted.id(), new ArrayList<>(List.of(new WhatsappProcessingStep(
                    UUID.randomUUID(),
                    persisted.id(),
                    persisted.status(),
                    null,
                    persisted.receivedAt(),
                    persisted.createdAt(),
                    persisted.updatedAt()
            ))));
            return persisted;
        }

        @Override
        public Optional<WhatsappInboundMessage> findByProviderMessageId(String providerMessageId) {
            return messagesById.values().stream()
                    .filter(message -> message.providerMessageId().equals(providerMessageId))
                    .findFirst();
        }

        @Override
        public Optional<WhatsappInboundMessage> findById(UUID inboundMessageId) {
            return Optional.ofNullable(messagesById.get(inboundMessageId));
        }

        @Override
        public List<WhatsappInboundMessage> findRecentByFarmId(UUID farmId, int limit) {
            return List.of();
        }

        @Override
        public List<WhatsappProcessingStep> findProcessingSteps(UUID inboundMessageId) {
            return stepsByMessageId.getOrDefault(inboundMessageId, List.of());
        }

        @Override
        public WhatsappInboundMessage recordProcessingStep(
                UUID inboundMessageId,
                WhatsappProcessingStatus status,
                String details,
                OffsetDateTime occurredAt
        ) {
            WhatsappInboundMessage current = messagesById.get(inboundMessageId);
            WhatsappInboundMessage updated = current.withProcessingStatus(status, details, occurredAt);
            messagesById.put(inboundMessageId, updated);
            stepsByMessageId.computeIfAbsent(inboundMessageId, ignored -> new ArrayList<>()).add(new WhatsappProcessingStep(
                    UUID.randomUUID(),
                    inboundMessageId,
                    status,
                    details,
                    occurredAt,
                    occurredAt,
                    occurredAt
            ));
            return updated;
        }
    }
}
