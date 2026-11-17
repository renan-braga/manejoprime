package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.whatsapp.application.WhatsappMessagePublishFailedException;
import br.com.curralzap.backend.whatsapp.application.WhatsappMessageReceivedEvent;
import br.com.curralzap.backend.whatsapp.application.WhatsappReceivedMessagePublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
class KafkaWhatsappReceivedMessagePublisher extends AbstractKafkaWhatsappEventPublisher<WhatsappMessageReceivedEvent>
        implements WhatsappReceivedMessagePublisher {

    KafkaWhatsappReceivedMessagePublisher(
            KafkaTemplate<String, WhatsappMessageReceivedEvent> kafkaTemplate,
            @Value("${curralzap.whatsapp.kafka.topics.message-received}") String topicName
    ) {
        super(kafkaTemplate, topicName);
    }

    @Override
    public void publishReceived(WhatsappMessageReceivedEvent event) {
        publish(event, (currentEvent, exception) ->
                new WhatsappMessagePublishFailedException(currentEvent.inboundMessageId(), currentEvent.providerMessageId(), exception));
    }
}
