package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.whatsapp.application.WhatsappOutboundDeliveryPublishFailedException;
import br.com.curralzap.backend.whatsapp.application.WhatsappOutboundDeliveryRequestedEvent;
import br.com.curralzap.backend.whatsapp.application.WhatsappOutboundDeliveryRequestedPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
class KafkaWhatsappOutboundDeliveryRequestedPublisher
        extends AbstractKafkaWhatsappEventPublisher<WhatsappOutboundDeliveryRequestedEvent>
        implements WhatsappOutboundDeliveryRequestedPublisher {

    KafkaWhatsappOutboundDeliveryRequestedPublisher(
            KafkaTemplate<String, WhatsappOutboundDeliveryRequestedEvent> kafkaTemplate,
            @Value("${curralzap.whatsapp.kafka.topics.outbound-delivery-requested}") String topicName
    ) {
        super(kafkaTemplate, topicName);
    }

    @Override
    public void publishRequested(WhatsappOutboundDeliveryRequestedEvent event) {
        publish(event, (currentEvent, exception) -> new WhatsappOutboundDeliveryPublishFailedException(
                currentEvent.inboundMessageId(),
                currentEvent.providerMessageId(),
                exception
        ));
    }
}
