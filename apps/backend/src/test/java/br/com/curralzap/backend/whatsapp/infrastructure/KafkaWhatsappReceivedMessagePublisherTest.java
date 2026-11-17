package br.com.curralzap.backend.whatsapp.infrastructure;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import br.com.curralzap.backend.whatsapp.application.WhatsappMessagePublishFailedException;
import br.com.curralzap.backend.whatsapp.application.WhatsappMessageReceivedEvent;
import br.com.curralzap.backend.whatsapp.domain.WhatsappProcessingStatus;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KafkaWhatsappReceivedMessagePublisherTest {

    @Test
    void publishesReceivedMessageEventToConfiguredTopic() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, WhatsappMessageReceivedEvent> kafkaTemplate = mock(KafkaTemplate.class);
        when(kafkaTemplate.send(eq("whatsapp.message.received"), eq("wamid.ABC123"), any(WhatsappMessageReceivedEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
        KafkaWhatsappReceivedMessagePublisher publisher =
                new KafkaWhatsappReceivedMessagePublisher(kafkaTemplate, "whatsapp.message.received");
        ListAppender<ILoggingEvent> appender = attachAppender(AbstractKafkaWhatsappEventPublisher.class);

        publisher.publishReceived(sampleEvent());

        verify(kafkaTemplate).send(eq("whatsapp.message.received"), eq("wamid.ABC123"), any(WhatsappMessageReceivedEvent.class));
        assertThat(appender.list).hasSize(2);
        assertThat(appender.list.getLast().getFormattedMessage()).contains("Published WhatsApp worker event");
        assertThat(appender.list.getLast().getMDCPropertyMap())
                .containsEntry("correlationId", "req-wa-003-test")
                .containsEntry("providerMessageId", "wamid.ABC123")
                .containsEntry("kafkaTopic", "whatsapp.message.received");
        detachAppender(AbstractKafkaWhatsappEventPublisher.class, appender);
    }

    @Test
    void wrapsBrokerFailureAsExplicitPublishException() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, WhatsappMessageReceivedEvent> kafkaTemplate = mock(KafkaTemplate.class);
        CompletableFuture<Object> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new IllegalStateException("Kafka unavailable"));
        when(kafkaTemplate.send(eq("whatsapp.message.received"), eq("wamid.ABC123"), any(WhatsappMessageReceivedEvent.class)))
                .thenReturn((CompletableFuture) failedFuture);
        KafkaWhatsappReceivedMessagePublisher publisher =
                new KafkaWhatsappReceivedMessagePublisher(kafkaTemplate, "whatsapp.message.received");

        assertThatThrownBy(() -> publisher.publishReceived(sampleEvent()))
                .isInstanceOf(WhatsappMessagePublishFailedException.class)
                .hasMessage("Failed to publish received WhatsApp message event.");
    }

    private static WhatsappMessageReceivedEvent sampleEvent() {
        UUID inboundMessageId = UUID.randomUUID();
        return new WhatsappMessageReceivedEvent(
                1,
                "whatsapp.message.received.v1",
                inboundMessageId.toString(),
                "req-wa-003-test",
                OffsetDateTime.parse("2026-04-04T10:00:00Z"),
                inboundMessageId,
                "wamid.ABC123",
                UUID.randomUUID(),
                UUID.randomUUID(),
                "5511999990001",
                "conversation",
                WhatsappProcessingStatus.RECEIVED
        );
    }

    private static ListAppender<ILoggingEvent> attachAppender(Class<?> type) {
        Logger logger = (Logger) LoggerFactory.getLogger(type);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private static void detachAppender(Class<?> type, ListAppender<ILoggingEvent> appender) {
        Logger logger = (Logger) LoggerFactory.getLogger(type);
        logger.detachAppender(appender);
    }
}
