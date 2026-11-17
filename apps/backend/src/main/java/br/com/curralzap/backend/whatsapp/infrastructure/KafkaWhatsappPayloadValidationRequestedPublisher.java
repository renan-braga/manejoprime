package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.whatsapp.application.WhatsappPayloadValidationPublishFailedException;
import br.com.curralzap.backend.whatsapp.application.WhatsappPayloadValidationRequestedEvent;
import br.com.curralzap.backend.whatsapp.application.WhatsappPayloadValidationRequestedPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
class KafkaWhatsappPayloadValidationRequestedPublisher
        extends AbstractKafkaWhatsappEventPublisher<WhatsappPayloadValidationRequestedEvent>
        implements WhatsappPayloadValidationRequestedPublisher {

    KafkaWhatsappPayloadValidationRequestedPublisher(
            KafkaTemplate<String, WhatsappPayloadValidationRequestedEvent> kafkaTemplate,
            @Value("${curralzap.whatsapp.kafka.topics.payload-validation-requested}") String topicName
    ) {
        super(kafkaTemplate, topicName);
    }

    @Override
    public void publishRequested(WhatsappPayloadValidationRequestedEvent event) {
        publish(event, (currentEvent, exception) -> new WhatsappPayloadValidationPublishFailedException(
                currentEvent.inboundMessageId(),
                currentEvent.providerMessageId(),
                exception
        ));
    }
}
