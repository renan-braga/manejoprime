package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.whatsapp.application.WhatsappTranscriptExtractionPublishFailedException;
import br.com.curralzap.backend.whatsapp.application.WhatsappTranscriptExtractionRequestedEvent;
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

class KafkaWhatsappTranscriptExtractionRequestedPublisherTest {

    @Test
    void publishesExtractionRequestedEventToConfiguredTopic() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, WhatsappTranscriptExtractionRequestedEvent> kafkaTemplate = mock(KafkaTemplate.class);
        when(kafkaTemplate.send(
                eq("whatsapp.transcript.extraction.requested"),
                eq("wamid.EXTRACT123"),
                any(WhatsappTranscriptExtractionRequestedEvent.class)
        )).thenReturn(CompletableFuture.completedFuture(null));
        KafkaWhatsappTranscriptExtractionRequestedPublisher publisher =
                new KafkaWhatsappTranscriptExtractionRequestedPublisher(
                        kafkaTemplate,
                        "whatsapp.transcript.extraction.requested"
                );

        publisher.publishRequested(sampleEvent());

        verify(kafkaTemplate).send(
                eq("whatsapp.transcript.extraction.requested"),
                eq("wamid.EXTRACT123"),
                any(WhatsappTranscriptExtractionRequestedEvent.class)
        );
    }

    @Test
    void wrapsBrokerFailureAsExplicitPublishException() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, WhatsappTranscriptExtractionRequestedEvent> kafkaTemplate = mock(KafkaTemplate.class);
        CompletableFuture<Object> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new IllegalStateException("Kafka unavailable"));
        when(kafkaTemplate.send(
                eq("whatsapp.transcript.extraction.requested"),
                eq("wamid.EXTRACT123"),
                any(WhatsappTranscriptExtractionRequestedEvent.class)
        )).thenReturn((CompletableFuture) failedFuture);
        KafkaWhatsappTranscriptExtractionRequestedPublisher publisher =
                new KafkaWhatsappTranscriptExtractionRequestedPublisher(
                        kafkaTemplate,
                        "whatsapp.transcript.extraction.requested"
                );

        assertThatThrownBy(() -> publisher.publishRequested(sampleEvent()))
                .isInstanceOf(WhatsappTranscriptExtractionPublishFailedException.class)
                .hasMessage("Failed to publish WhatsApp transcript extraction request event.");
    }

    private static WhatsappTranscriptExtractionRequestedEvent sampleEvent() {
        UUID inboundMessageId = UUID.randomUUID();
        return new WhatsappTranscriptExtractionRequestedEvent(
                1,
                "whatsapp.transcript.extraction.requested.v1",
                inboundMessageId.toString(),
                "req-wa-007-publisher",
                OffsetDateTime.parse("2026-04-05T15:30:00Z"),
                inboundMessageId,
                "wamid.EXTRACT123"
        );
    }
}
