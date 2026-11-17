package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.shared.infrastructure.logging.CurralzapLogContext;
import br.com.curralzap.backend.shared.infrastructure.logging.LogContextScope;
import br.com.curralzap.backend.whatsapp.application.WhatsappWorkerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class WhatsappWorkerExecutionTemplate {

    private static final Logger LOGGER = LoggerFactory.getLogger(WhatsappWorkerExecutionTemplate.class);

    void handle(String topicName, String consumerGroup, WhatsappWorkerEvent event, Runnable action) {
        long startedAt = System.nanoTime();
        try (LogContextScope ignored = CurralzapLogContext.withWhatsappEvent(event, topicName, consumerGroup)) {
            LOGGER.info("Handling WhatsApp worker event");
            try {
                action.run();
                LOGGER.info("Handled WhatsApp worker event durationMs={}", (System.nanoTime() - startedAt) / 1_000_000);
            } catch (RuntimeException exception) {
                LOGGER.warn(
                        "WhatsApp worker event failed durationMs={} failureType={} failureMessage={}",
                        (System.nanoTime() - startedAt) / 1_000_000,
                        exception.getClass().getSimpleName(),
                        exception.getMessage(),
                        exception
                );
                throw exception;
            }
        }
    }
}
