package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.whatsapp.application.WhatsappButtonConfirmationRequestedEvent;
import br.com.curralzap.backend.whatsapp.application.WhatsappConfirmationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Profile("worker")
@Component
class KafkaWhatsappConfirmationWorker {

    private final WhatsappConfirmationService confirmationService;
    private final WhatsappWorkerExecutionTemplate workerExecutionTemplate;
    private final String topicName;
    private final String consumerGroup;

    KafkaWhatsappConfirmationWorker(
            WhatsappConfirmationService confirmationService,
            WhatsappWorkerExecutionTemplate workerExecutionTemplate,
            @Value("${curralzap.whatsapp.kafka.topics.button-confirmation-requested}") String topicName,
            @Value("${curralzap.whatsapp.kafka.consumer-groups.button-confirmation}") String consumerGroup
    ) {
        this.confirmationService = confirmationService;
        this.workerExecutionTemplate = workerExecutionTemplate;
        this.topicName = topicName;
        this.consumerGroup = consumerGroup;
    }

    @KafkaListener(
            topics = "${curralzap.whatsapp.kafka.topics.button-confirmation-requested}",
            groupId = "${curralzap.whatsapp.kafka.consumer-groups.button-confirmation}",
            containerFactory = "whatsappButtonConfirmationRequestedKafkaListenerContainerFactory"
    )
    void handleButtonConfirmationRequested(WhatsappButtonConfirmationRequestedEvent event) {
        workerExecutionTemplate.handle(topicName, consumerGroup, event, () -> confirmationService.handleButtonConfirmation(event));
    }
}
