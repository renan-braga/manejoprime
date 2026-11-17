package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.whatsapp.application.WhatsappAudioDownloadService;
import br.com.curralzap.backend.whatsapp.application.WhatsappMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("worker")
@Component
class KafkaWhatsappAudioDownloadWorker {

    private final WhatsappAudioDownloadService audioDownloadService;
    private final WhatsappWorkerExecutionTemplate workerExecutionTemplate;
    private final String topicName;
    private final String consumerGroup;

    KafkaWhatsappAudioDownloadWorker(
            WhatsappAudioDownloadService audioDownloadService,
            WhatsappWorkerExecutionTemplate workerExecutionTemplate,
            @Value("${curralzap.whatsapp.kafka.topics.message-received}") String topicName,
            @Value("${curralzap.whatsapp.kafka.consumer-groups.audio-download}") String consumerGroup
    ) {
        this.audioDownloadService = audioDownloadService;
        this.workerExecutionTemplate = workerExecutionTemplate;
        this.topicName = topicName;
        this.consumerGroup = consumerGroup;
    }

    @KafkaListener(
            topics = "${curralzap.whatsapp.kafka.topics.message-received}",
            groupId = "${curralzap.whatsapp.kafka.consumer-groups.audio-download}",
            containerFactory = "whatsappMessageReceivedKafkaListenerContainerFactory"
    )
    void handleReceivedMessage(WhatsappMessageReceivedEvent event) {
        workerExecutionTemplate.handle(topicName, consumerGroup, event, () -> audioDownloadService.handleReceivedMessage(event));
    }
}
