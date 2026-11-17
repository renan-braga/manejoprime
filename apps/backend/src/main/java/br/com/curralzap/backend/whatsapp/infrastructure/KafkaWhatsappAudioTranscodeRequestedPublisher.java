package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.whatsapp.application.WhatsappAudioTranscodePublishFailedException;
import br.com.curralzap.backend.whatsapp.application.WhatsappAudioTranscodeRequestedEvent;
import br.com.curralzap.backend.whatsapp.application.WhatsappAudioTranscodeRequestedPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
class KafkaWhatsappAudioTranscodeRequestedPublisher
        extends AbstractKafkaWhatsappEventPublisher<WhatsappAudioTranscodeRequestedEvent>
        implements WhatsappAudioTranscodeRequestedPublisher {

    KafkaWhatsappAudioTranscodeRequestedPublisher(
            KafkaTemplate<String, WhatsappAudioTranscodeRequestedEvent> kafkaTemplate,
            @Value("${curralzap.whatsapp.kafka.topics.audio-transcode-requested}") String topicName
    ) {
        super(kafkaTemplate, topicName);
    }

    @Override
    public void publishRequested(WhatsappAudioTranscodeRequestedEvent event) {
        publish(event, (currentEvent, exception) -> new WhatsappAudioTranscodePublishFailedException(
                currentEvent.inboundMessageId(),
                currentEvent.providerMessageId(),
                exception
        ));
    }
}
