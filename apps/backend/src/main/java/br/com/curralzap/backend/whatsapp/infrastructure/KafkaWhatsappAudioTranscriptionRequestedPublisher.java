package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.whatsapp.application.WhatsappAudioTranscriptionPublishFailedException;
import br.com.curralzap.backend.whatsapp.application.WhatsappAudioTranscriptionRequestedEvent;
import br.com.curralzap.backend.whatsapp.application.WhatsappAudioTranscriptionRequestedPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
class KafkaWhatsappAudioTranscriptionRequestedPublisher
        extends AbstractKafkaWhatsappEventPublisher<WhatsappAudioTranscriptionRequestedEvent>
        implements WhatsappAudioTranscriptionRequestedPublisher {

    KafkaWhatsappAudioTranscriptionRequestedPublisher(
            KafkaTemplate<String, WhatsappAudioTranscriptionRequestedEvent> kafkaTemplate,
            @Value("${curralzap.whatsapp.kafka.topics.audio-transcription-requested}") String topicName
    ) {
        super(kafkaTemplate, topicName);
    }

    @Override
    public void publishRequested(WhatsappAudioTranscriptionRequestedEvent event) {
        publish(event, (currentEvent, exception) -> new WhatsappAudioTranscriptionPublishFailedException(
                currentEvent.inboundMessageId(),
                currentEvent.providerMessageId(),
                exception
        ));
    }
}
