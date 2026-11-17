package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.whatsapp.application.WhatsappTranscriptExtractionPublishFailedException;
import br.com.curralzap.backend.whatsapp.application.WhatsappTranscriptExtractionRequestedEvent;
import br.com.curralzap.backend.whatsapp.application.WhatsappTranscriptExtractionRequestedPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
class KafkaWhatsappTranscriptExtractionRequestedPublisher
        extends AbstractKafkaWhatsappEventPublisher<WhatsappTranscriptExtractionRequestedEvent>
        implements WhatsappTranscriptExtractionRequestedPublisher {

    KafkaWhatsappTranscriptExtractionRequestedPublisher(
            KafkaTemplate<String, WhatsappTranscriptExtractionRequestedEvent> kafkaTemplate,
            @Value("${curralzap.whatsapp.kafka.topics.transcript-extraction-requested}") String topicName
    ) {
        super(kafkaTemplate, topicName);
    }

    @Override
    public void publishRequested(WhatsappTranscriptExtractionRequestedEvent event) {
        publish(event, (currentEvent, exception) -> new WhatsappTranscriptExtractionPublishFailedException(
                currentEvent.inboundMessageId(),
                currentEvent.providerMessageId(),
                exception
        ));
    }
}
