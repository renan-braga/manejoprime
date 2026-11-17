package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.shared.infrastructure.logging.CurralzapLogContext;
import br.com.curralzap.backend.shared.infrastructure.logging.LogContextScope;
import br.com.curralzap.backend.whatsapp.application.WhatsappWorkerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

abstract class AbstractKafkaWhatsappEventPublisher<T extends WhatsappWorkerEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractKafkaWhatsappEventPublisher.class);

    private final KafkaTemplate<String, T> kafkaTemplate;
    private final String topicName;

    AbstractKafkaWhatsappEventPublisher(KafkaTemplate<String, T> kafkaTemplate, String topicName) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    protected void publish(T event, PublishFailureFactory<T> failureFactory) {
        try (LogContextScope ignored = CurralzapLogContext.withWhatsappEvent(event, topicName, null)) {
            LOGGER.info("Publishing WhatsApp worker event");
            kafkaTemplate.send(topicName, event.providerMessageId(), event).get(5, TimeUnit.SECONDS);
            LOGGER.info("Published WhatsApp worker event");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Publishing WhatsApp worker event interrupted", exception);
            throw failureFactory.create(event, exception);
        } catch (ExecutionException | TimeoutException exception) {
            LOGGER.warn("Publishing WhatsApp worker event failed", exception);
            throw failureFactory.create(event, exception);
        }
    }

    @FunctionalInterface
    protected interface PublishFailureFactory<T extends WhatsappWorkerEvent> {

        RuntimeException create(T event, Exception exception);
    }
}
