package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.whatsapp.application.WhatsappAudioTranscriptionPublishFailedException;
import br.com.curralzap.backend.whatsapp.application.WhatsappAudioTranscriptionRequestedEvent;
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

class KafkaWhatsappAudioTranscriptionRequestedPublisherTest {

    @Test
    void publishesTranscriptionRequestedEventToConfiguredTopic() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, WhatsappAudioTranscriptionRequestedEvent> kafkaTemplate = mock(KafkaTemplate.class);
        when(kafkaTemplate.send(
                eq("whatsapp.audio.transcription.requested"),
                eq("wamid.TRANSCRIBE123"),
                any(WhatsappAudioTranscriptionRequestedEvent.class)
        )).thenReturn(CompletableFuture.completedFuture(null));
        KafkaWhatsappAudioTranscriptionRequestedPublisher publisher =
                new KafkaWhatsappAudioTranscriptionRequestedPublisher(
                        kafkaTemplate,
                        "whatsapp.audio.transcription.requested"
                );

        publisher.publishRequested(sampleEvent());

        verify(kafkaTemplate).send(
                eq("whatsapp.audio.transcription.requested"),
                eq("wamid.TRANSCRIBE123"),
                any(WhatsappAudioTranscriptionRequestedEvent.class)
        );
    }

    @Test
    void wrapsBrokerFailureAsExplicitPublishException() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, WhatsappAudioTranscriptionRequestedEvent> kafkaTemplate = mock(KafkaTemplate.class);
        CompletableFuture<Object> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new IllegalStateException("Kafka unavailable"));
        when(kafkaTemplate.send(
                eq("whatsapp.audio.transcription.requested"),
                eq("wamid.TRANSCRIBE123"),
                any(WhatsappAudioTranscriptionRequestedEvent.class)
        )).thenReturn((CompletableFuture) failedFuture);
        KafkaWhatsappAudioTranscriptionRequestedPublisher publisher =
                new KafkaWhatsappAudioTranscriptionRequestedPublisher(
                        kafkaTemplate,
                        "whatsapp.audio.transcription.requested"
                );

        assertThatThrownBy(() -> publisher.publishRequested(sampleEvent()))
                .isInstanceOf(WhatsappAudioTranscriptionPublishFailedException.class)
                .hasMessage("Failed to publish WhatsApp audio transcription request event.");
    }

    private static WhatsappAudioTranscriptionRequestedEvent sampleEvent() {
        UUID inboundMessageId = UUID.randomUUID();
        return new WhatsappAudioTranscriptionRequestedEvent(
                1,
                "whatsapp.audio.transcription.requested.v1",
                inboundMessageId.toString(),
                "req-wa-006-publisher",
                OffsetDateTime.parse("2026-04-04T12:30:00Z"),
                inboundMessageId,
                "wamid.TRANSCRIBE123",
                "whatsapp/audio/wamid.TRANSCRIBE123.mp3",
                "audio/mpeg"
        );
    }
}
