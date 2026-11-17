package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.whatsapp.application.WhatsappOutboundDeliveryPublishFailedException;
import br.com.curralzap.backend.whatsapp.application.WhatsappOutboundDeliveryRequestedEvent;
import br.com.curralzap.backend.whatsapp.domain.WhatsappProcessingStatus;
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

class KafkaWhatsappOutboundDeliveryRequestedPublisherTest {

    @Test
    void publishesOutboundDeliveryRequestedEventToConfiguredTopic() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, WhatsappOutboundDeliveryRequestedEvent> kafkaTemplate = mock(KafkaTemplate.class);
        when(kafkaTemplate.send(
                eq("whatsapp.outbound.delivery.requested"),
                eq("wamid.OUTBOUND123"),
                any(WhatsappOutboundDeliveryRequestedEvent.class)
        )).thenReturn(CompletableFuture.completedFuture(null));
        KafkaWhatsappOutboundDeliveryRequestedPublisher publisher =
                new KafkaWhatsappOutboundDeliveryRequestedPublisher(
                        kafkaTemplate,
                        "whatsapp.outbound.delivery.requested"
                );

        publisher.publishRequested(sampleEvent());

        verify(kafkaTemplate).send(
                eq("whatsapp.outbound.delivery.requested"),
                eq("wamid.OUTBOUND123"),
                any(WhatsappOutboundDeliveryRequestedEvent.class)
        );
    }

    @Test
    void wrapsBrokerFailureAsExplicitPublishException() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, WhatsappOutboundDeliveryRequestedEvent> kafkaTemplate = mock(KafkaTemplate.class);
        CompletableFuture<Object> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new IllegalStateException("Kafka unavailable"));
        when(kafkaTemplate.send(
                eq("whatsapp.outbound.delivery.requested"),
                eq("wamid.OUTBOUND123"),
                any(WhatsappOutboundDeliveryRequestedEvent.class)
        )).thenReturn((CompletableFuture) failedFuture);
        KafkaWhatsappOutboundDeliveryRequestedPublisher publisher =
                new KafkaWhatsappOutboundDeliveryRequestedPublisher(
                        kafkaTemplate,
                        "whatsapp.outbound.delivery.requested"
                );

        assertThatThrownBy(() -> publisher.publishRequested(sampleEvent()))
                .isInstanceOf(WhatsappOutboundDeliveryPublishFailedException.class)
                .hasMessage("Failed to publish WhatsApp outbound delivery request event.");
    }

    private static WhatsappOutboundDeliveryRequestedEvent sampleEvent() {
        UUID inboundMessageId = UUID.randomUUID();
        return new WhatsappOutboundDeliveryRequestedEvent(
                1,
                "whatsapp.outbound.delivery.requested.v1",
                inboundMessageId.toString(),
                "req-wa-009-publisher",
                OffsetDateTime.parse("2026-04-29T12:30:00Z"),
                inboundMessageId,
                "wamid.OUTBOUND123",
                WhatsappProcessingStatus.AWAITING_CONFIRMATION
        );
    }
}
