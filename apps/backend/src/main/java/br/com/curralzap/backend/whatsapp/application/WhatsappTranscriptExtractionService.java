package br.com.curralzap.backend.whatsapp.application;

import br.com.curralzap.backend.whatsapp.domain.WhatsappExtractedPayload;
import br.com.curralzap.backend.whatsapp.domain.WhatsappInboundMessage;
import br.com.curralzap.backend.whatsapp.domain.WhatsappProcessingStatus;
import br.com.curralzap.backend.whatsapp.domain.WhatsappTranscript;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class WhatsappTranscriptExtractionService {

    private static final double LOW_CONFIDENCE_THRESHOLD = 0.70;

    private final WhatsappInboundMessageRepository inboundMessageRepository;
    private final WhatsappTranscriptRepository transcriptRepository;
    private final WhatsappExtractedPayloadRepository extractedPayloadRepository;
    private final WhatsappTranscriptExtractor transcriptExtractor;
    private final WhatsappPayloadValidationRequestedPublisher payloadValidationRequestedPublisher;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public WhatsappTranscriptExtractionService(
            WhatsappInboundMessageRepository inboundMessageRepository,
            WhatsappTranscriptRepository transcriptRepository,
            WhatsappExtractedPayloadRepository extractedPayloadRepository,
            WhatsappTranscriptExtractor transcriptExtractor,
            WhatsappPayloadValidationRequestedPublisher payloadValidationRequestedPublisher,
            ObjectMapper objectMapper
    ) {
        this(
                inboundMessageRepository,
                transcriptRepository,
                extractedPayloadRepository,
                transcriptExtractor,
                payloadValidationRequestedPublisher,
                objectMapper,
                Clock.systemUTC()
        );
    }

    WhatsappTranscriptExtractionService(
            WhatsappInboundMessageRepository inboundMessageRepository,
            WhatsappTranscriptRepository transcriptRepository,
            WhatsappExtractedPayloadRepository extractedPayloadRepository,
            WhatsappTranscriptExtractor transcriptExtractor,
            Clock clock
    ) {
        this(
                inboundMessageRepository,
                transcriptRepository,
                extractedPayloadRepository,
                transcriptExtractor,
                ignored -> {
                },
                new ObjectMapper(),
                clock
        );
    }

    WhatsappTranscriptExtractionService(
            WhatsappInboundMessageRepository inboundMessageRepository,
            WhatsappTranscriptRepository transcriptRepository,
            WhatsappExtractedPayloadRepository extractedPayloadRepository,
            WhatsappTranscriptExtractor transcriptExtractor,
            WhatsappPayloadValidationRequestedPublisher payloadValidationRequestedPublisher,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.inboundMessageRepository = inboundMessageRepository;
        this.transcriptRepository = transcriptRepository;
        this.extractedPayloadRepository = extractedPayloadRepository;
        this.transcriptExtractor = transcriptExtractor;
        this.payloadValidationRequestedPublisher = payloadValidationRequestedPublisher;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public void handleRequestedExtraction(WhatsappTranscriptExtractionRequestedEvent event) {
        WhatsappInboundMessage inboundMessage = inboundMessageRepository.findByProviderMessageId(event.providerMessageId())
                .orElseThrow(() -> new IllegalStateException(
                        "Inbound message not found for provider message ID: " + event.providerMessageId()
                ));
        if (inboundMessage.status() == WhatsappProcessingStatus.AWAITING_CONFIRMATION
                || inboundMessage.status() == WhatsappProcessingStatus.VALIDATED) {
            return;
        }
        if (inboundMessage.status() == WhatsappProcessingStatus.INTENT_EXTRACTED
                || extractedPayloadRepository.findByInboundMessageId(event.inboundMessageId()).isPresent()) {
            publishPayloadValidationRequest(inboundMessage, event.correlationId());
            return;
        }

        WhatsappTranscript transcript = transcriptRepository.findByInboundMessageId(event.inboundMessageId())
                .orElseThrow(() -> new WhatsappWorkerRetryableException(
                        event.inboundMessageId(),
                        event.providerMessageId(),
                        "Transcript not yet visible for inbound message ID: " + event.inboundMessageId() + " — retrying after transcription transaction commit",
                        new IllegalStateException("Transcript not found")
                ));

        try {
            WhatsappTranscriptExtractionResult result = transcriptExtractor.extract(new WhatsappTranscriptExtractionRequest(
                    inboundMessage.providerMessageId(),
                    transcript.transcript()
            ));
            validateResult(result);
            StoredExtraction storedExtraction = storedExtraction(result);
            extractedPayloadRepository.save(WhatsappExtractedPayload.extracted(
                    inboundMessage.id(),
                    result.provider(),
                    result.providerModel(),
                    storedExtraction.intent(),
                    toJson(storedExtraction.payload()),
                    OffsetDateTime.now(clock)
            ));
            inboundMessageRepository.recordProcessingStep(
                    inboundMessage.id(),
                    WhatsappProcessingStatus.INTENT_EXTRACTED,
                    storedExtraction.details(),
                    OffsetDateTime.now(clock)
            );
            publishPayloadValidationRequest(inboundMessage, event.correlationId());
        } catch (RuntimeException exception) {
            recordFailure(inboundMessage, failureReason(exception));
            if (exception instanceof WhatsappWorkerRetryableException retryableException) {
                throw retryableException;
            }
            throw new WhatsappWorkerRetryableException(
                    inboundMessage.id(),
                    inboundMessage.providerMessageId(),
                    "Failed to process WhatsApp transcript extraction worker event.",
                    exception
            );
        }
    }

    private void validateResult(WhatsappTranscriptExtractionResult result) {
        if (result == null || result.provider() == null || result.provider().isBlank()
                || result.providerModel() == null || result.providerModel().isBlank()
                || result.intent() == null || result.intent().isBlank()
                || result.confidence() == null) {
            throw new IllegalStateException("Extraction provider returned a malformed result.");
        }
    }

    private StoredExtraction storedExtraction(WhatsappTranscriptExtractionResult result) {
        if (result.isUnknown()) {
            Map<String, Object> payload = basePayload("unknown", result.confidence());
            if (result.reason() != null && !result.reason().isBlank()) {
                payload.put("reason", result.reason());
            }
            return new StoredExtraction(
                    "unknown",
                    payload,
                    "Transcript extraction produced unknown intent."
            );
        }

        if (result.confidence() < LOW_CONFIDENCE_THRESHOLD) {
            Map<String, Object> payload = basePayload("low_confidence", result.confidence());
            payload.put("candidateIntent", result.intent());
            payload.put("fields", result.fields());
            return new StoredExtraction(
                    "low_confidence",
                    payload,
                    "Transcript extraction produced low confidence result for " + result.intent() + "."
            );
        }

        Map<String, Object> payload = basePayload(result.intent(), result.confidence());
        payload.putAll(result.fields());
        return new StoredExtraction(
                result.intent(),
                payload,
                "Transcript extracted as " + result.intent() + "."
        );
    }

    private Map<String, Object> basePayload(String intent, double confidence) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("intent", intent);
        payload.put("confidence", confidence);
        return payload;
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize transcript extraction payload.", exception);
        }
    }

    private void recordFailure(WhatsappInboundMessage inboundMessage, String reason) {
        inboundMessageRepository.recordProcessingStep(
                inboundMessage.id(),
                WhatsappProcessingStatus.FAILED,
                "Transcript extraction failed: " + reason,
                OffsetDateTime.now(clock)
        );
    }

    private String failureReason(RuntimeException exception) {
        if (exception.getMessage() == null || exception.getMessage().isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return exception.getMessage();
    }

    private void publishPayloadValidationRequest(WhatsappInboundMessage inboundMessage, String correlationId) {
        try {
            payloadValidationRequestedPublisher.publishRequested(new WhatsappPayloadValidationRequestedEvent(
                    1,
                    "whatsapp.payload.validation.requested.v1",
                    inboundMessage.id().toString(),
                    correlationId,
                    OffsetDateTime.now(clock),
                    inboundMessage.id(),
                    inboundMessage.providerMessageId()
            ));
        } catch (RuntimeException exception) {
            throw new WhatsappPayloadValidationPublishFailedException(
                    inboundMessage.id(),
                    inboundMessage.providerMessageId(),
                    exception
            );
        }
    }

    private record StoredExtraction(String intent, Map<String, Object> payload, String details) {
    }
}
