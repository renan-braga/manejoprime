package br.com.curralzap.backend.whatsapp.infrastructure;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import br.com.curralzap.backend.whatsapp.application.WhatsappMessageReceivedEvent;
import br.com.curralzap.backend.whatsapp.domain.WhatsappProcessingStatus;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WhatsappWorkerExecutionTemplateTest {

    @Test
    void logsWorkerHandlingWithCorrelationContext() {
        WhatsappWorkerExecutionTemplate template = new WhatsappWorkerExecutionTemplate();
        WhatsappMessageReceivedEvent event = sampleEvent();
        ListAppender<ILoggingEvent> appender = attachAppender(WhatsappWorkerExecutionTemplate.class);

        template.handle("whatsapp.message.received", "curralzap-whatsapp-audio-download", event, () ->
                assertThat(MDC.get("correlationId")).isEqualTo("req-wa-010-log-1"));

        assertThat(MDC.get("correlationId")).isNull();
        assertThat(appender.list).hasSize(2);
        assertThat(appender.list.getFirst().getMDCPropertyMap())
                .containsEntry("correlationId", "req-wa-010-log-1")
                .containsEntry("providerMessageId", "wamid.LOG123")
                .containsEntry("kafkaTopic", "whatsapp.message.received")
                .containsEntry("kafkaGroupId", "curralzap-whatsapp-audio-download");
        assertThat(appender.list.getFirst().getFormattedMessage()).contains("Handling WhatsApp worker event");
        assertThat(appender.list.getLast().getFormattedMessage()).contains("Handled WhatsApp worker event");
        detachAppender(WhatsappWorkerExecutionTemplate.class, appender);
    }

    @Test
    void logsWorkerFailureWithCorrelationContext() {
        WhatsappWorkerExecutionTemplate template = new WhatsappWorkerExecutionTemplate();
        WhatsappMessageReceivedEvent event = sampleEvent();
        ListAppender<ILoggingEvent> appender = attachAppender(WhatsappWorkerExecutionTemplate.class);

        assertThatThrownBy(() -> template.handle(
                "whatsapp.message.received",
                "curralzap-whatsapp-audio-download",
                event,
                () -> {
                    throw new IllegalStateException("boom");
                }
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");

        assertThat(appender.list).hasSize(2);
        assertThat(appender.list.getLast().getFormattedMessage()).contains("WhatsApp worker event failed");
        assertThat(appender.list.getLast().getMDCPropertyMap()).containsEntry("correlationId", "req-wa-010-log-1");
        detachAppender(WhatsappWorkerExecutionTemplate.class, appender);
    }

    private static WhatsappMessageReceivedEvent sampleEvent() {
        UUID inboundMessageId = UUID.randomUUID();
        return new WhatsappMessageReceivedEvent(
                1,
                "whatsapp.message.received.v1",
                inboundMessageId.toString(),
                "req-wa-010-log-1",
                OffsetDateTime.parse("2026-05-01T12:00:00Z"),
                inboundMessageId,
                "wamid.LOG123",
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
