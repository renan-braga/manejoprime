package br.com.curralzap.backend.whatsapp.application;

import br.com.curralzap.backend.whatsapp.domain.WhatsappAudioMedia;
import br.com.curralzap.backend.whatsapp.domain.WhatsappInboundMessage;
import br.com.curralzap.backend.whatsapp.domain.WhatsappProcessingStatus;
import br.com.curralzap.backend.whatsapp.domain.WhatsappTranscript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;

@Service
public class WhatsappAudioTranscriptionService {

    private static final Logger log = LoggerFactory.getLogger(WhatsappAudioTranscriptionService.class);

    private final WhatsappInboundMessageRepository inboundMessageRepository;
    private final WhatsappAudioMediaRepository audioMediaRepository;
    private final WhatsappTranscriptRepository transcriptRepository;
    private final WhatsappAudioStorage audioStorage;
    private final WhatsappAudioTranscriber audioTranscriber;
    private final WhatsappTranscriptExtractionRequestedPublisher extractionRequestedPublisher;
    private final Clock clock;

    @Autowired
    public WhatsappAudioTranscriptionService(
            WhatsappInboundMessageRepository inboundMessageRepository,
            WhatsappAudioMediaRepository audioMediaRepository,
            WhatsappTranscriptRepository transcriptRepository,
            WhatsappAudioStorage audioStorage,
            WhatsappAudioTranscriber audioTranscriber,
            WhatsappTranscriptExtractionRequestedPublisher extractionRequestedPublisher
    ) {
        this(
                inboundMessageRepository,
                audioMediaRepository,
                transcriptRepository,
                audioStorage,
                audioTranscriber,
                extractionRequestedPublisher,
                Clock.systemUTC()
        );
    }

    WhatsappAudioTranscriptionService(
            WhatsappInboundMessageRepository inboundMessageRepository,
            WhatsappAudioMediaRepository audioMediaRepository,
            WhatsappTranscriptRepository transcriptRepository,
            WhatsappAudioStorage audioStorage,
            WhatsappAudioTranscriber audioTranscriber,
            WhatsappTranscriptExtractionRequestedPublisher extractionRequestedPublisher,
            Clock clock
    ) {
        this.inboundMessageRepository = inboundMessageRepository;
        this.audioMediaRepository = audioMediaRepository;
        this.transcriptRepository = transcriptRepository;
        this.audioStorage = audioStorage;
        this.audioTranscriber = audioTranscriber;
        this.extractionRequestedPublisher = extractionRequestedPublisher;
        this.clock = clock;
    }

    @Transactional
    public void handleRequestedTranscription(WhatsappAudioTranscriptionRequestedEvent event) {
        WhatsappInboundMessage inboundMessage = inboundMessageRepository.findByProviderMessageId(event.providerMessageId())
                .orElseThrow(() -> new IllegalStateException(
                        "Inbound message not found for provider message ID: " + event.providerMessageId()
                ));
        if (inboundMessage.status() == WhatsappProcessingStatus.INTENT_EXTRACTED) {
            return;
        }
        if (inboundMessage.status() == WhatsappProcessingStatus.TRANSCRIBED
                || transcriptRepository.findByInboundMessageId(event.inboundMessageId()).isPresent()) {
            publishExtractionRequest(inboundMessage, event.correlationId());
            return;
        }

        WhatsappAudioMedia audioMedia = audioMediaRepository.findByInboundMessageId(event.inboundMessageId())
                .orElseThrow(() -> new IllegalStateException(
                        "Transcoded audio media not found for inbound message ID: " + event.inboundMessageId()
                ));
        if (audioMedia.transcodedStoragePath() == null || audioMedia.transcodedMimeType() == null) {
            recordFailure(inboundMessage, "Transcoded audio is not available.");
            return;
        }

        try {
            byte[] transcodedContent = audioStorage.load(audioMedia.transcodedStoragePath());
            WhatsappAudioTranscriptionResult result = audioTranscriber.transcribe(new WhatsappAudioTranscriptionRequest(
                    inboundMessage.providerMessageId(),
                    audioMedia.transcodedMimeType(),
                    transcodedContent
            ));
            validateResult(result);
            log.info("Audio transcribed providerMessageId={} transcript={}", inboundMessage.providerMessageId(), result.transcript());

            transcriptRepository.save(WhatsappTranscript.transcribed(
                    inboundMessage.id(),
                    result.provider(),
                    result.providerModel(),
                    result.transcript(),
                    OffsetDateTime.now(clock)
            ));
            inboundMessageRepository.recordProcessingStep(
                    inboundMessage.id(),
                    WhatsappProcessingStatus.TRANSCRIBED,
                    "Audio transcribed: " + result.transcript(),
                    OffsetDateTime.now(clock)
            );
            publishExtractionRequest(inboundMessage, event.correlationId());
        } catch (RuntimeException exception) {
            recordFailure(inboundMessage, failureReason(exception));
            if (exception instanceof WhatsappWorkerRetryableException retryableException) {
                throw retryableException;
            }
            throw new WhatsappWorkerRetryableException(
                    inboundMessage.id(),
                    inboundMessage.providerMessageId(),
                    "Failed to process WhatsApp audio transcription worker event.",
                    exception
            );
        }
    }

    private void validateResult(WhatsappAudioTranscriptionResult result) {
        if (result == null || result.transcript() == null || result.transcript().isBlank()) {
            throw new IllegalStateException("Transcription provider returned an empty transcript.");
        }
    }

    private void recordFailure(WhatsappInboundMessage inboundMessage, String reason) {
        inboundMessageRepository.recordProcessingStep(
                inboundMessage.id(),
                WhatsappProcessingStatus.FAILED,
                "Audio transcription failed: " + reason,
                OffsetDateTime.now(clock)
        );
    }

    private String failureReason(RuntimeException exception) {
        if (exception.getMessage() == null || exception.getMessage().isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return exception.getMessage();
    }

    private void publishExtractionRequest(WhatsappInboundMessage inboundMessage, String correlationId) {
        try {
            extractionRequestedPublisher.publishRequested(new WhatsappTranscriptExtractionRequestedEvent(
                    1,
                    "whatsapp.transcript.extraction.requested.v1",
                    inboundMessage.id().toString(),
                    correlationId,
                    OffsetDateTime.now(clock),
                    inboundMessage.id(),
                    inboundMessage.providerMessageId()
            ));
        } catch (RuntimeException exception) {
            throw new WhatsappTranscriptExtractionPublishFailedException(
                    inboundMessage.id(),
                    inboundMessage.providerMessageId(),
                    exception
            );
        }
    }
}
