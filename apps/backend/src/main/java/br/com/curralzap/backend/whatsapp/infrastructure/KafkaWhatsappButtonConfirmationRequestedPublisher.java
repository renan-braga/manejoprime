package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.whatsapp.application.WhatsappButtonConfirmationPublishFailedException;
import br.com.curralzap.backend.whatsapp.application.WhatsappButtonConfirmationRequestedEvent;
import br.com.curralzap.backend.whatsapp.application.WhatsappButtonConfirmationRequestedPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
class KafkaWhatsappButtonConfirmationRequestedPublisher
        extends AbstractKafkaWhatsappEventPublisher<WhatsappButtonConfirmationRequestedEvent>
        implements WhatsappButtonConfirmationRequestedPublisher {

    KafkaWhatsappButtonConfirmationRequestedPublisher(
            KafkaTemplate<String, WhatsappButtonConfirmationRequestedEvent> kafkaTemplate,
            @Value("${curralzap.whatsapp.kafka.topics.button-confirmation-requested}") String topicName
    ) {
        super(kafkaTemplate, topicName);
    }

    @Override
    public void publishRequested(WhatsappButtonConfirmationRequestedEvent event) {
        publish(event, (currentEvent, exception) -> new WhatsappButtonConfirmationPublishFailedException(
                currentEvent.inboundMessageId(),
                currentEvent.providerMessageId(),
                exception
        ));
    }
}
