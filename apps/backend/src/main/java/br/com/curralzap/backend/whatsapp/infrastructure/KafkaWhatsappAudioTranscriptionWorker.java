package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.whatsapp.application.WhatsappAudioTranscriptionRequestedEvent;
import br.com.curralzap.backend.whatsapp.application.WhatsappAudioTranscriptionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("worker")
@Component
class KafkaWhatsappAudioTranscriptionWorker {

    private final WhatsappAudioTranscriptionService audioTranscriptionService;
    private final WhatsappWorkerExecutionTemplate workerExecutionTemplate;
    private final String topicName;
    private final String consumerGroup;

    KafkaWhatsappAudioTranscriptionWorker(
            WhatsappAudioTranscriptionService audioTranscriptionService,
            WhatsappWorkerExecutionTemplate workerExecutionTemplate,
            @Value("${curralzap.whatsapp.kafka.topics.audio-transcription-requested}") String topicName,
            @Value("${curralzap.whatsapp.kafka.consumer-groups.audio-transcription}") String consumerGroup
    ) {
        this.audioTranscriptionService = audioTranscriptionService;
        this.workerExecutionTemplate = workerExecutionTemplate;
        this.topicName = topicName;
        this.consumerGroup = consumerGroup;
    }

    @KafkaListener(
            topics = "${curralzap.whatsapp.kafka.topics.audio-transcription-requested}",
            groupId = "${curralzap.whatsapp.kafka.consumer-groups.audio-transcription}",
            containerFactory = "whatsappAudioTranscriptionRequestedKafkaListenerContainerFactory"
    )
    void handleTranscriptionRequested(WhatsappAudioTranscriptionRequestedEvent event) {
        workerExecutionTemplate.handle(topicName, consumerGroup, event, () -> audioTranscriptionService.handleRequestedTranscription(event));
    }
}
