package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.shared.infrastructure.logging.CurralzapLogContext;
import br.com.curralzap.backend.shared.infrastructure.logging.LogContextScope;
import br.com.curralzap.backend.whatsapp.application.WhatsappInboundMessageRepository;
import br.com.curralzap.backend.whatsapp.application.WhatsappWorkerEvent;
import br.com.curralzap.backend.whatsapp.domain.WhatsappProcessingStatus;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Map;

class WhatsappKafkaDeadLetterRecoverer implements ConsumerRecordRecoverer {

    private static final Logger LOGGER = LoggerFactory.getLogger(WhatsappKafkaDeadLetterRecoverer.class);

    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final WhatsappInboundMessageRepository inboundMessageRepository;
    private final Map<String, String> deadLetterTopics;
    private final Clock clock;

    WhatsappKafkaDeadLetterRecoverer(
            KafkaTemplate<Object, Object> kafkaTemplate,
            WhatsappInboundMessageRepository inboundMessageRepository,
            Map<String, String> deadLetterTopics
    ) {
        this(kafkaTemplate, inboundMessageRepository, deadLetterTopics, Clock.systemUTC());
    }

    WhatsappKafkaDeadLetterRecoverer(
            KafkaTemplate<Object, Object> kafkaTemplate,
            WhatsappInboundMessageRepository inboundMessageRepository,
            Map<String, String> deadLetterTopics,
            Clock clock
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.inboundMessageRepository = inboundMessageRepository;
        this.deadLetterTopics = deadLetterTopics;
        this.clock = clock;
    }

    @Override
    public void accept(ConsumerRecord<?, ?> record, Exception exception) {
        logDeadLetter(record, exception);
        recordDeadLetterFailure(record.value(), record.topic(), exception);
        kafkaTemplate.send(deadLetterTopic(record.topic()), key(record.key()), record.value());
    }

    private void logDeadLetter(ConsumerRecord<?, ?> record, Exception exception) {
        Object value = record.value();
        if (value instanceof WhatsappWorkerEvent event) {
            try (LogContextScope ignored = CurralzapLogContext.withWhatsappEvent(event, record.topic(), null)) {
                LOGGER.error("Dead-lettering WhatsApp worker event targetTopic={}", deadLetterTopic(record.topic()), exception);
            }
            return;
        }
        LOGGER.error("Dead-lettering Kafka record sourceTopic={} targetTopic={}", record.topic(), deadLetterTopic(record.topic()), exception);
    }

    private void recordDeadLetterFailure(Object value, String sourceTopic, Exception exception) {
        if (!(value instanceof WhatsappWorkerEvent event)) {
            return;
        }
        try {
            inboundMessageRepository.recordProcessingStep(
                    event.inboundMessageId(),
                    WhatsappProcessingStatus.FAILED,
                    "Worker dead-lettered from " + sourceTopic + ": " + failureReason(exception),
                    OffsetDateTime.now(clock)
            );
        } catch (RuntimeException ignored) {
        }
    }

    private String deadLetterTopic(String sourceTopic) {
        return deadLetterTopics.getOrDefault(sourceTopic, sourceTopic + ".dlt");
    }

    private String key(Object key) {
        return key == null ? null : key.toString();
    }

    private String failureReason(Throwable exception) {
        Throwable current = exception;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        if (current.getMessage() == null || current.getMessage().isBlank()) {
            return current.getClass().getSimpleName();
        }
        return current.getMessage();
    }
}
