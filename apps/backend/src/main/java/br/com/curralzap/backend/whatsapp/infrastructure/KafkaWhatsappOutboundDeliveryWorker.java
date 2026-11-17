package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.whatsapp.application.WhatsappOutboundDeliveryRequestedEvent;
import br.com.curralzap.backend.whatsapp.application.WhatsappOutboundDeliveryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("worker")
@Component
class KafkaWhatsappOutboundDeliveryWorker {

    private final WhatsappOutboundDeliveryService outboundDeliveryService;
    private final WhatsappWorkerExecutionTemplate workerExecutionTemplate;
    private final String topicName;
    private final String consumerGroup;

    KafkaWhatsappOutboundDeliveryWorker(
            WhatsappOutboundDeliveryService outboundDeliveryService,
            WhatsappWorkerExecutionTemplate workerExecutionTemplate,
            @Value("${curralzap.whatsapp.kafka.topics.outbound-delivery-requested}") String topicName,
            @Value("${curralzap.whatsapp.kafka.consumer-groups.outbound-delivery}") String consumerGroup
    ) {
        this.outboundDeliveryService = outboundDeliveryService;
        this.workerExecutionTemplate = workerExecutionTemplate;
        this.topicName = topicName;
        this.consumerGroup = consumerGroup;
    }

    @KafkaListener(
            topics = "${curralzap.whatsapp.kafka.topics.outbound-delivery-requested}",
            groupId = "${curralzap.whatsapp.kafka.consumer-groups.outbound-delivery}",
            containerFactory = "whatsappOutboundDeliveryRequestedKafkaListenerContainerFactory"
    )
    void handleOutboundDeliveryRequested(WhatsappOutboundDeliveryRequestedEvent event) {
        workerExecutionTemplate.handle(topicName, consumerGroup, event, () -> outboundDeliveryService.handleRequestedDelivery(event));
    }
}
