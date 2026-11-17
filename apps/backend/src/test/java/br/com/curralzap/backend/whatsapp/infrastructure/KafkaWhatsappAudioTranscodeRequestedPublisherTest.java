package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.whatsapp.application.WhatsappAudioTranscodePublishFailedException;
import br.com.curralzap.backend.whatsapp.application.WhatsappAudioTranscodeRequestedEvent;
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

class KafkaWhatsappAudioTranscodeRequestedPublisherTest {

    @Test
    void publishesTranscodeRequestedEventToConfiguredTopic() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, WhatsappAudioTranscodeRequestedEvent> kafkaTemplate = mock(KafkaTemplate.class);
        when(kafkaTemplate.send(
                eq("whatsapp.audio.transcode.requested"),
                eq("wamid.ABC123"),
                any(WhatsappAudioTranscodeRequestedEvent.class)
        )).thenReturn(CompletableFuture.completedFuture(null));
        KafkaWhatsappAudioTranscodeRequestedPublisher publisher =
                new KafkaWhatsappAudioTranscodeRequestedPublisher(
                        kafkaTemplate,
                        "whatsapp.audio.transcode.requested"
                );

        publisher.publishRequested(sampleEvent());

        verify(kafkaTemplate).send(
                eq("whatsapp.audio.transcode.requested"),
                eq("wamid.ABC123"),
                any(WhatsappAudioTranscodeRequestedEvent.class)
        );
    }

    @Test
    void wrapsBrokerFailureAsExplicitPublishException() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, WhatsappAudioTranscodeRequestedEvent> kafkaTemplate = mock(KafkaTemplate.class);
        CompletableFuture<Object> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new IllegalStateException("Kafka unavailable"));
        when(kafkaTemplate.send(
                eq("whatsapp.audio.transcode.requested"),
                eq("wamid.ABC123"),
                any(WhatsappAudioTranscodeRequestedEvent.class)
        )).thenReturn((CompletableFuture) failedFuture);
        KafkaWhatsappAudioTranscodeRequestedPublisher publisher =
                new KafkaWhatsappAudioTranscodeRequestedPublisher(
                        kafkaTemplate,
                        "whatsapp.audio.transcode.requested"
                );

        assertThatThrownBy(() -> publisher.publishRequested(sampleEvent()))
                .isInstanceOf(WhatsappAudioTranscodePublishFailedException.class)
                .hasMessage("Failed to publish WhatsApp audio transcode request event.");
    }

    private static WhatsappAudioTranscodeRequestedEvent sampleEvent() {
        UUID inboundMessageId = UUID.randomUUID();
        return new WhatsappAudioTranscodeRequestedEvent(
                1,
                "whatsapp.audio.transcode.requested.v1",
                inboundMessageId.toString(),
                "req-wa-005-publisher",
                OffsetDateTime.parse("2026-04-04T12:00:00Z"),
                inboundMessageId,
                "wamid.ABC123",
                "whatsapp/audio/wamid.ABC123.ogg",
                "audio/ogg"
        );
    }
}
