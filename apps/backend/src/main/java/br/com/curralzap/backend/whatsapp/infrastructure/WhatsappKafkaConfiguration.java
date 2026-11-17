package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.whatsapp.application.WhatsappAudioTranscodeRequestedEvent;
import br.com.curralzap.backend.whatsapp.application.WhatsappAudioTranscriptionRequestedEvent;
import br.com.curralzap.backend.whatsapp.application.WhatsappButtonConfirmationRequestedEvent;
import br.com.curralzap.backend.whatsapp.application.WhatsappMessageReceivedEvent;
import br.com.curralzap.backend.whatsapp.application.WhatsappOutboundDeliveryRequestedEvent;
import br.com.curralzap.backend.whatsapp.application.WhatsappPayloadValidationRequestedEvent;
import br.com.curralzap.backend.whatsapp.application.WhatsappTranscriptExtractionRequestedEvent;
import br.com.curralzap.backend.whatsapp.application.WhatsappInboundMessageRepository;
import br.com.curralzap.backend.whatsapp.application.WhatsappWorkerRetryableException;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Map;

@Profile("worker")
@Configuration
class WhatsappKafkaConfiguration {

    @Bean
    NewTopic whatsappMessageReceivedTopic(
            @Value("${curralzap.whatsapp.kafka.topics.message-received}") String topicName
    ) {
        return TopicBuilder.name(topicName)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic whatsappAudioTranscodeRequestedTopic(
            @Value("${curralzap.whatsapp.kafka.topics.audio-transcode-requested}") String topicName
    ) {
        return TopicBuilder.name(topicName)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic whatsappAudioTranscriptionRequestedTopic(
            @Value("${curralzap.whatsapp.kafka.topics.audio-transcription-requested}") String topicName
    ) {
        return TopicBuilder.name(topicName)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic whatsappTranscriptExtractionRequestedTopic(
            @Value("${curralzap.whatsapp.kafka.topics.transcript-extraction-requested}") String topicName
    ) {
        return TopicBuilder.name(topicName)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic whatsappPayloadValidationRequestedTopic(
            @Value("${curralzap.whatsapp.kafka.topics.payload-validation-requested}") String topicName
    ) {
        return TopicBuilder.name(topicName)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic whatsappOutboundDeliveryRequestedTopic(
            @Value("${curralzap.whatsapp.kafka.topics.outbound-delivery-requested}") String topicName
    ) {
        return topic(topicName);
    }

    @Bean
    NewTopic whatsappButtonConfirmationRequestedTopic(
            @Value("${curralzap.whatsapp.kafka.topics.button-confirmation-requested}") String topicName
    ) {
        return topic(topicName);
    }

    @Bean
    NewTopic whatsappMessageReceivedDeadLetterTopic(
            @Value("${curralzap.whatsapp.kafka.topics.message-received}") String topicName,
            @Value("${curralzap.whatsapp.kafka.retry.dead-letter-suffix:.dlt}") String deadLetterSuffix
    ) {
        return topic(topicName + deadLetterSuffix);
    }

    @Bean
    NewTopic whatsappAudioTranscodeRequestedDeadLetterTopic(
            @Value("${curralzap.whatsapp.kafka.topics.audio-transcode-requested}") String topicName,
            @Value("${curralzap.whatsapp.kafka.retry.dead-letter-suffix:.dlt}") String deadLetterSuffix
    ) {
        return topic(topicName + deadLetterSuffix);
    }

    @Bean
    NewTopic whatsappAudioTranscriptionRequestedDeadLetterTopic(
            @Value("${curralzap.whatsapp.kafka.topics.audio-transcription-requested}") String topicName,
            @Value("${curralzap.whatsapp.kafka.retry.dead-letter-suffix:.dlt}") String deadLetterSuffix
    ) {
        return topic(topicName + deadLetterSuffix);
    }

    @Bean
    NewTopic whatsappTranscriptExtractionRequestedDeadLetterTopic(
            @Value("${curralzap.whatsapp.kafka.topics.transcript-extraction-requested}") String topicName,
            @Value("${curralzap.whatsapp.kafka.retry.dead-letter-suffix:.dlt}") String deadLetterSuffix
    ) {
        return topic(topicName + deadLetterSuffix);
    }

    @Bean
    NewTopic whatsappPayloadValidationRequestedDeadLetterTopic(
            @Value("${curralzap.whatsapp.kafka.topics.payload-validation-requested}") String topicName,
            @Value("${curralzap.whatsapp.kafka.retry.dead-letter-suffix:.dlt}") String deadLetterSuffix
    ) {
        return topic(topicName + deadLetterSuffix);
    }

    @Bean
    NewTopic whatsappOutboundDeliveryRequestedDeadLetterTopic(
            @Value("${curralzap.whatsapp.kafka.topics.outbound-delivery-requested}") String topicName,
            @Value("${curralzap.whatsapp.kafka.retry.dead-letter-suffix:.dlt}") String deadLetterSuffix
    ) {
        return topic(topicName + deadLetterSuffix);
    }

    @Bean
    NewTopic whatsappButtonConfirmationRequestedDeadLetterTopic(
            @Value("${curralzap.whatsapp.kafka.topics.button-confirmation-requested}") String topicName,
            @Value("${curralzap.whatsapp.kafka.retry.dead-letter-suffix:.dlt}") String deadLetterSuffix
    ) {
        return topic(topicName + deadLetterSuffix);
    }

    @Bean
    Map<String, String> whatsappDeadLetterTopics(
            @Value("${curralzap.whatsapp.kafka.topics.message-received}") String messageReceivedTopic,
            @Value("${curralzap.whatsapp.kafka.topics.audio-transcode-requested}") String audioTranscodeRequestedTopic,
            @Value("${curralzap.whatsapp.kafka.topics.audio-transcription-requested}") String audioTranscriptionRequestedTopic,
            @Value("${curralzap.whatsapp.kafka.topics.transcript-extraction-requested}") String transcriptExtractionRequestedTopic,
            @Value("${curralzap.whatsapp.kafka.topics.payload-validation-requested}") String payloadValidationRequestedTopic,
            @Value("${curralzap.whatsapp.kafka.topics.outbound-delivery-requested}") String outboundDeliveryRequestedTopic,
            @Value("${curralzap.whatsapp.kafka.topics.button-confirmation-requested}") String buttonConfirmationRequestedTopic,
            @Value("${curralzap.whatsapp.kafka.retry.dead-letter-suffix:.dlt}") String deadLetterSuffix
    ) {
        return Map.of(
                messageReceivedTopic, messageReceivedTopic + deadLetterSuffix,
                audioTranscodeRequestedTopic, audioTranscodeRequestedTopic + deadLetterSuffix,
                audioTranscriptionRequestedTopic, audioTranscriptionRequestedTopic + deadLetterSuffix,
                transcriptExtractionRequestedTopic, transcriptExtractionRequestedTopic + deadLetterSuffix,
                payloadValidationRequestedTopic, payloadValidationRequestedTopic + deadLetterSuffix,
                outboundDeliveryRequestedTopic, outboundDeliveryRequestedTopic + deadLetterSuffix,
                buttonConfirmationRequestedTopic, buttonConfirmationRequestedTopic + deadLetterSuffix
        );
    }

    @Bean
    WhatsappKafkaDeadLetterRecoverer whatsappKafkaDeadLetterRecoverer(
            KafkaTemplate<Object, Object> kafkaTemplate,
            WhatsappInboundMessageRepository inboundMessageRepository,
            Map<String, String> whatsappDeadLetterTopics
    ) {
        return new WhatsappKafkaDeadLetterRecoverer(kafkaTemplate, inboundMessageRepository, whatsappDeadLetterTopics);
    }

    @Bean
    DefaultErrorHandler whatsappWorkerKafkaErrorHandler(
            WhatsappKafkaDeadLetterRecoverer deadLetterRecoverer,
            @Value("${curralzap.whatsapp.kafka.retry.interval-ms:1000}") long retryIntervalMs,
            @Value("${curralzap.whatsapp.kafka.retry.max-attempts:3}") long retryMaxAttempts
    ) {
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                deadLetterRecoverer,
                new FixedBackOff(retryIntervalMs, Math.max(0, retryMaxAttempts - 1))
        );
        errorHandler.setCommitRecovered(true);
        errorHandler.setClassifications(Map.of(WhatsappWorkerRetryableException.class, true), false);
        return errorHandler;
    }

    @Bean
    ConsumerFactory<String, WhatsappMessageReceivedEvent> whatsappMessageReceivedConsumerFactory(
            KafkaProperties kafkaProperties
    ) {
        return new DefaultKafkaConsumerFactory<>(
                kafkaProperties.buildConsumerProperties(null),
                new StringDeserializer(),
                jsonDeserializer(WhatsappMessageReceivedEvent.class)
        );
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, WhatsappMessageReceivedEvent>
    whatsappMessageReceivedKafkaListenerContainerFactory(
            ConsumerFactory<String, WhatsappMessageReceivedEvent> whatsappMessageReceivedConsumerFactory,
            KafkaProperties kafkaProperties,
            DefaultErrorHandler whatsappWorkerKafkaErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, WhatsappMessageReceivedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(whatsappMessageReceivedConsumerFactory);
        factory.setAutoStartup(kafkaProperties.getListener().isAutoStartup());
        factory.setCommonErrorHandler(whatsappWorkerKafkaErrorHandler);
        return factory;
    }

    @Bean
    ConsumerFactory<String, WhatsappAudioTranscodeRequestedEvent> whatsappAudioTranscodeRequestedConsumerFactory(
            KafkaProperties kafkaProperties
    ) {
        return new DefaultKafkaConsumerFactory<>(
                kafkaProperties.buildConsumerProperties(null),
                new StringDeserializer(),
                jsonDeserializer(WhatsappAudioTranscodeRequestedEvent.class)
        );
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, WhatsappAudioTranscodeRequestedEvent>
    whatsappAudioTranscodeRequestedKafkaListenerContainerFactory(
            ConsumerFactory<String, WhatsappAudioTranscodeRequestedEvent> whatsappAudioTranscodeRequestedConsumerFactory,
            KafkaProperties kafkaProperties,
            DefaultErrorHandler whatsappWorkerKafkaErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, WhatsappAudioTranscodeRequestedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(whatsappAudioTranscodeRequestedConsumerFactory);
        factory.setAutoStartup(kafkaProperties.getListener().isAutoStartup());
        factory.setCommonErrorHandler(whatsappWorkerKafkaErrorHandler);
        return factory;
    }

    @Bean
    ConsumerFactory<String, WhatsappAudioTranscriptionRequestedEvent> whatsappAudioTranscriptionRequestedConsumerFactory(
            KafkaProperties kafkaProperties
    ) {
        return new DefaultKafkaConsumerFactory<>(
                kafkaProperties.buildConsumerProperties(null),
                new StringDeserializer(),
                jsonDeserializer(WhatsappAudioTranscriptionRequestedEvent.class)
        );
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, WhatsappAudioTranscriptionRequestedEvent>
    whatsappAudioTranscriptionRequestedKafkaListenerContainerFactory(
            ConsumerFactory<String, WhatsappAudioTranscriptionRequestedEvent> whatsappAudioTranscriptionRequestedConsumerFactory,
            KafkaProperties kafkaProperties,
            DefaultErrorHandler whatsappWorkerKafkaErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, WhatsappAudioTranscriptionRequestedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(whatsappAudioTranscriptionRequestedConsumerFactory);
        factory.setAutoStartup(kafkaProperties.getListener().isAutoStartup());
        factory.setCommonErrorHandler(whatsappWorkerKafkaErrorHandler);
        return factory;
    }

    @Bean
    ConsumerFactory<String, WhatsappTranscriptExtractionRequestedEvent> whatsappTranscriptExtractionRequestedConsumerFactory(
            KafkaProperties kafkaProperties
    ) {
        return new DefaultKafkaConsumerFactory<>(
                kafkaProperties.buildConsumerProperties(null),
                new StringDeserializer(),
                jsonDeserializer(WhatsappTranscriptExtractionRequestedEvent.class)
        );
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, WhatsappTranscriptExtractionRequestedEvent>
    whatsappTranscriptExtractionRequestedKafkaListenerContainerFactory(
            ConsumerFactory<String, WhatsappTranscriptExtractionRequestedEvent> whatsappTranscriptExtractionRequestedConsumerFactory,
            KafkaProperties kafkaProperties,
            DefaultErrorHandler whatsappWorkerKafkaErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, WhatsappTranscriptExtractionRequestedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(whatsappTranscriptExtractionRequestedConsumerFactory);
        factory.setAutoStartup(kafkaProperties.getListener().isAutoStartup());
        factory.setCommonErrorHandler(whatsappWorkerKafkaErrorHandler);
        return factory;
    }

    @Bean
    ConsumerFactory<String, WhatsappPayloadValidationRequestedEvent> whatsappPayloadValidationRequestedConsumerFactory(
            KafkaProperties kafkaProperties
    ) {
        return new DefaultKafkaConsumerFactory<>(
                kafkaProperties.buildConsumerProperties(null),
                new StringDeserializer(),
                jsonDeserializer(WhatsappPayloadValidationRequestedEvent.class)
        );
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, WhatsappPayloadValidationRequestedEvent>
    whatsappPayloadValidationRequestedKafkaListenerContainerFactory(
            ConsumerFactory<String, WhatsappPayloadValidationRequestedEvent> whatsappPayloadValidationRequestedConsumerFactory,
            KafkaProperties kafkaProperties,
            DefaultErrorHandler whatsappWorkerKafkaErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, WhatsappPayloadValidationRequestedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(whatsappPayloadValidationRequestedConsumerFactory);
        factory.setAutoStartup(kafkaProperties.getListener().isAutoStartup());
        factory.setCommonErrorHandler(whatsappWorkerKafkaErrorHandler);
        return factory;
    }

    @Bean
    ConsumerFactory<String, WhatsappOutboundDeliveryRequestedEvent> whatsappOutboundDeliveryRequestedConsumerFactory(
            KafkaProperties kafkaProperties
    ) {
        return new DefaultKafkaConsumerFactory<>(
                kafkaProperties.buildConsumerProperties(null),
                new StringDeserializer(),
                jsonDeserializer(WhatsappOutboundDeliveryRequestedEvent.class)
        );
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, WhatsappOutboundDeliveryRequestedEvent>
    whatsappOutboundDeliveryRequestedKafkaListenerContainerFactory(
            ConsumerFactory<String, WhatsappOutboundDeliveryRequestedEvent> whatsappOutboundDeliveryRequestedConsumerFactory,
            KafkaProperties kafkaProperties,
            DefaultErrorHandler whatsappWorkerKafkaErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, WhatsappOutboundDeliveryRequestedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(whatsappOutboundDeliveryRequestedConsumerFactory);
        factory.setAutoStartup(kafkaProperties.getListener().isAutoStartup());
        factory.setCommonErrorHandler(whatsappWorkerKafkaErrorHandler);
        return factory;
    }

    @Bean
    ConsumerFactory<String, WhatsappButtonConfirmationRequestedEvent> whatsappButtonConfirmationRequestedConsumerFactory(
            KafkaProperties kafkaProperties
    ) {
        return new DefaultKafkaConsumerFactory<>(
                kafkaProperties.buildConsumerProperties(null),
                new StringDeserializer(),
                jsonDeserializer(WhatsappButtonConfirmationRequestedEvent.class)
        );
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, WhatsappButtonConfirmationRequestedEvent>
    whatsappButtonConfirmationRequestedKafkaListenerContainerFactory(
            ConsumerFactory<String, WhatsappButtonConfirmationRequestedEvent> whatsappButtonConfirmationRequestedConsumerFactory,
            KafkaProperties kafkaProperties,
            DefaultErrorHandler whatsappWorkerKafkaErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, WhatsappButtonConfirmationRequestedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(whatsappButtonConfirmationRequestedConsumerFactory);
        factory.setAutoStartup(kafkaProperties.getListener().isAutoStartup());
        factory.setCommonErrorHandler(whatsappWorkerKafkaErrorHandler);
        return factory;
    }

    private NewTopic topic(String topicName) {
        return TopicBuilder.name(topicName)
                .partitions(1)
                .replicas(1)
                .build();
    }

    private <T> JsonDeserializer<T> jsonDeserializer(Class<T> targetClass) {
        JsonDeserializer<T> deserializer = new JsonDeserializer<>(targetClass, false);
        deserializer.addTrustedPackages("br.com.curralzap.backend.whatsapp.application");
        return deserializer;
    }
}
