package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.whatsapp.application.WhatsappAudioTranscodeRequestedEvent;
import br.com.curralzap.backend.whatsapp.application.WhatsappAudioTranscodeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("worker")
@Component
class KafkaWhatsappAudioTranscodeWorker {

    private final WhatsappAudioTranscodeService audioTranscodeService;
    private final WhatsappWorkerExecutionTemplate workerExecutionTemplate;
    private final String topicName;
    private final String consumerGroup;

    KafkaWhatsappAudioTranscodeWorker(
            WhatsappAudioTranscodeService audioTranscodeService,
            WhatsappWorkerExecutionTemplate workerExecutionTemplate,
            @Value("${curralzap.whatsapp.kafka.topics.audio-transcode-requested}") String topicName,
            @Value("${curralzap.whatsapp.kafka.consumer-groups.audio-transcode}") String consumerGroup
    ) {
        this.audioTranscodeService = audioTranscodeService;
        this.workerExecutionTemplate = workerExecutionTemplate;
        this.topicName = topicName;
        this.consumerGroup = consumerGroup;
    }

    @KafkaListener(
            topics = "${curralzap.whatsapp.kafka.topics.audio-transcode-requested}",
            groupId = "${curralzap.whatsapp.kafka.consumer-groups.audio-transcode}",
            containerFactory = "whatsappAudioTranscodeRequestedKafkaListenerContainerFactory"
    )
    void handleTranscodeRequested(WhatsappAudioTranscodeRequestedEvent event) {
        workerExecutionTemplate.handle(topicName, consumerGroup, event, () -> audioTranscodeService.handleRequestedTranscode(event));
    }
}
