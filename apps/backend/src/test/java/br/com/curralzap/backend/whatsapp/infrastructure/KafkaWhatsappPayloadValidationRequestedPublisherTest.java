package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.whatsapp.application.WhatsappPayloadValidationPublishFailedException;
import br.com.curralzap.backend.whatsapp.application.WhatsappPayloadValidationRequestedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KafkaWhatsappPayloadValidationRequestedPublisherTest {

    @Test
    void publishesValidationRequestedEventToConfiguredTopic() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, WhatsappPayloadValidationRequestedEvent> kafkaTemplate = mock(KafkaTemplate.class);
        when(kafkaTemplate.send(
                eq("whatsapp.payload.validation.requested"),
                eq("wamid.VALIDATE123"),
                any(WhatsappPayloadValidationRequestedEvent.class)
        )).thenReturn(CompletableFuture.completedFuture(null));
        KafkaWhatsappPayloadValidationRequestedPublisher publisher =
                new KafkaWhatsappPayloadValidationRequestedPublisher(
                        kafkaTemplate,
                        "whatsapp.payload.validation.requested"
                );

        publisher.publishRequested(sampleEvent());

        verify(kafkaTemplate).send(
                eq("whatsapp.payload.validation.requested"),
                eq("wamid.VALIDATE123"),
                any(WhatsappPayloadValidationRequestedEvent.class)
        );
    }

    @Test
    void wrapsBrokerFailureAsExplicitPublishException() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, WhatsappPayloadValidationRequestedEvent> kafkaTemplate = mock(KafkaTemplate.class);
        CompletableFuture<Object> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new IllegalStateException("Kafka unavailable"));
        when(kafkaTemplate.send(
                eq("whatsapp.payload.validation.requested"),
                eq("wamid.VALIDATE123"),
                any(WhatsappPayloadValidationRequestedEvent.class)
        )).thenReturn((CompletableFuture) failedFuture);
        KafkaWhatsappPayloadValidationRequestedPublisher publisher =
                new KafkaWhatsappPayloadValidationRequestedPublisher(
                        kafkaTemplate,
                        "whatsapp.payload.validation.requested"
                );

        assertThatThrownBy(() -> publisher.publishRequested(sampleEvent()))
                .isInstanceOf(WhatsappPayloadValidationPublishFailedException.class)
                .hasMessage("Failed to publish WhatsApp payload validation request event.");
    }

    private static WhatsappPayloadValidationRequestedEvent sampleEvent() {
        UUID inboundMessageId = UUID.randomUUID();
        return new WhatsappPayloadValidationRequestedEvent(
                1,
                "whatsapp.payload.validation.requested.v1",
                inboundMessageId.toString(),
                "req-wa-008-publisher",
                OffsetDateTime.parse("2026-04-06T10:30:00Z"),
                inboundMessageId,
                "wamid.VALIDATE123"
        );
    }
}
