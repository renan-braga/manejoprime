package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.whatsapp.application.WhatsappPayloadValidationRequestedEvent;
import br.com.curralzap.backend.whatsapp.application.WhatsappPayloadValidationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("worker")
@Component
class KafkaWhatsappPayloadValidationWorker {

    private final WhatsappPayloadValidationService payloadValidationService;
    private final WhatsappWorkerExecutionTemplate workerExecutionTemplate;
    private final String topicName;
    private final String consumerGroup;

    KafkaWhatsappPayloadValidationWorker(
            WhatsappPayloadValidationService payloadValidationService,
            WhatsappWorkerExecutionTemplate workerExecutionTemplate,
            @Value("${curralzap.whatsapp.kafka.topics.payload-validation-requested}") String topicName,
            @Value("${curralzap.whatsapp.kafka.consumer-groups.payload-validation}") String consumerGroup
    ) {
        this.payloadValidationService = payloadValidationService;
        this.workerExecutionTemplate = workerExecutionTemplate;
        this.topicName = topicName;
        this.consumerGroup = consumerGroup;
    }

    @KafkaListener(
            topics = "${curralzap.whatsapp.kafka.topics.payload-validation-requested}",
            groupId = "${curralzap.whatsapp.kafka.consumer-groups.payload-validation}",
            containerFactory = "whatsappPayloadValidationRequestedKafkaListenerContainerFactory"
    )
    void handleValidationRequested(WhatsappPayloadValidationRequestedEvent event) {
        workerExecutionTemplate.handle(topicName, consumerGroup, event, () -> payloadValidationService.handleRequestedValidation(event));
    }
}
