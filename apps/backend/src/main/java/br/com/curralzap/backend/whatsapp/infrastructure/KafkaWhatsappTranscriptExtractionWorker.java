package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.whatsapp.application.WhatsappTranscriptExtractionRequestedEvent;
import br.com.curralzap.backend.whatsapp.application.WhatsappTranscriptExtractionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("worker")
@Component
class KafkaWhatsappTranscriptExtractionWorker {

    private final WhatsappTranscriptExtractionService transcriptExtractionService;
    private final WhatsappWorkerExecutionTemplate workerExecutionTemplate;
    private final String topicName;
    private final String consumerGroup;

    KafkaWhatsappTranscriptExtractionWorker(
            WhatsappTranscriptExtractionService transcriptExtractionService,
            WhatsappWorkerExecutionTemplate workerExecutionTemplate,
            @Value("${curralzap.whatsapp.kafka.topics.transcript-extraction-requested}") String topicName,
            @Value("${curralzap.whatsapp.kafka.consumer-groups.transcript-extraction}") String consumerGroup
    ) {
        this.transcriptExtractionService = transcriptExtractionService;
        this.workerExecutionTemplate = workerExecutionTemplate;
        this.topicName = topicName;
        this.consumerGroup = consumerGroup;
    }

    @KafkaListener(
            topics = "${curralzap.whatsapp.kafka.topics.transcript-extraction-requested}",
            groupId = "${curralzap.whatsapp.kafka.consumer-groups.transcript-extraction}",
            containerFactory = "whatsappTranscriptExtractionRequestedKafkaListenerContainerFactory"
    )
    void handleExtractionRequested(WhatsappTranscriptExtractionRequestedEvent event) {
        workerExecutionTemplate.handle(topicName, consumerGroup, event, () -> transcriptExtractionService.handleRequestedExtraction(event));
    }
}
