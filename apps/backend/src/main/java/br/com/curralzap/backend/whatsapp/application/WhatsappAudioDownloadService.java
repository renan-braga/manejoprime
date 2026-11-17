package br.com.curralzap.backend.whatsapp.application;

import br.com.curralzap.backend.whatsapp.domain.WhatsappAudioMedia;
import br.com.curralzap.backend.whatsapp.domain.WhatsappInboundMessage;
import br.com.curralzap.backend.whatsapp.domain.WhatsappProcessingStatus;
import br.com.curralzap.backend.whatsapp.domain.WhatsappTranscript;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;

@Service
public class WhatsappAudioDownloadService {

    private final WhatsappInboundMessageRepository inboundMessageRepository;
    private final WhatsappAudioMediaRepository audioMediaRepository;
    private final WhatsappAudioPayloadExtractor payloadExtractor;
    private final WhatsappAudioDownloadGateway downloadGateway;
    private final WhatsappAudioStorage audioStorage;
    private final WhatsappAudioTranscodeRequestedPublisher transcodeRequestedPublisher;
    private final WhatsappTranscriptRepository transcriptRepository;
    private final WhatsappTranscriptExtractionRequestedPublisher extractionRequestedPublisher;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public WhatsappAudioDownloadService(
            WhatsappInboundMessageRepository inboundMessageRepository,
            WhatsappAudioMediaRepository audioMediaRepository,
            WhatsappAudioPayloadExtractor payloadExtractor,
            WhatsappAudioDownloadGateway downloadGateway,
            WhatsappAudioStorage audioStorage,
            WhatsappAudioTranscodeRequestedPublisher transcodeRequestedPublisher,
            WhatsappTranscriptRepository transcriptRepository,
            WhatsappTranscriptExtractionRequestedPublisher extractionRequestedPublisher,
            ObjectMapper objectMapper
    ) {
        this(
                inboundMessageRepository,
                audioMediaRepository,
                payloadExtractor,
                downloadGateway,
                audioStorage,
                transcodeRequestedPublisher,
                transcriptRepository,
                extractionRequestedPublisher,
                objectMapper,
                Clock.systemUTC()
        );
    }

    WhatsappAudioDownloadService(
            WhatsappInboundMessageRepository inboundMessageRepository,
            WhatsappAudioMediaRepository audioMediaRepository,
            WhatsappAudioPayloadExtractor payloadExtractor,
            WhatsappAudioDownloadGateway downloadGateway,
            WhatsappAudioStorage audioStorage,
            WhatsappAudioTranscodeRequestedPublisher transcodeRequestedPublisher,
            WhatsappTranscriptRepository transcriptRepository,
            WhatsappTranscriptExtractionRequestedPublisher extractionRequestedPublisher,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.inboundMessageRepository = inboundMessageRepository;
        this.audioMediaRepository = audioMediaRepository;
        this.payloadExtractor = payloadExtractor;
        this.downloadGateway = downloadGateway;
        this.audioStorage = audioStorage;
        this.transcodeRequestedPublisher = transcodeRequestedPublisher;
        this.transcriptRepository = transcriptRepository;
        this.extractionRequestedPublisher = extractionRequestedPublisher;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public void handleReceivedMessage(WhatsappMessageReceivedEvent event) {
        if ("conversation".equals(event.messageType()) || "extendedTextMessage".equals(event.messageType())) {
            handleTextMessage(event);
            return;
        }
        if (!"audioMessage".equals(event.messageType())) {
            return;
        }

        WhatsappInboundMessage inboundMessage = inboundMessageRepository.findByProviderMessageId(event.providerMessageId())
                .orElseThrow(() -> new IllegalStateException(
                        "Inbound message not found for provider message ID: " + event.providerMessageId()
                ));

        if (inboundMessage.status() == WhatsappProcessingStatus.TRANSCODED) {
            return;
        }

        var existingAudio = audioMediaRepository.findByInboundMessageId(inboundMessage.id());
        if (existingAudio.isPresent()) {
            if (existingAudio.get().transcodedAt() != null) {
                return;
            }
            publishTranscodeRequest(existingAudio.get(), inboundMessage, event.correlationId());
            return;
        }

        var payload = payloadExtractor.extract(inboundMessage);
        if (payload.isEmpty()) {
            recordFailure(inboundMessage, "Audio payload has no downloadable URL or inline content.");
            return;
        }

        try {
            DownloadedWhatsappAudio downloadedAudio = resolveAudio(payload.get());
            String resolvedMimeType = resolveMimeType(payload.get(), downloadedAudio);
            String storagePath = audioStorage.store(new StoreWhatsappAudioRequest(
                    inboundMessage.id(),
                    inboundMessage.providerMessageId(),
                    inboundMessage.providerMessageId(),
                    resolvedMimeType,
                    downloadedAudio.content()
            ));

            WhatsappAudioMedia savedAudioMedia = audioMediaRepository.save(WhatsappAudioMedia.downloaded(
                    inboundMessage.id(),
                    payload.get().mediaUrl(),
                    resolvedMimeType,
                    storagePath,
                    OffsetDateTime.now(clock)
            ));

            inboundMessageRepository.recordProcessingStep(
                    inboundMessage.id(),
                    WhatsappProcessingStatus.MEDIA_DOWNLOADED,
                    "Audio downloaded to " + storagePath + ".",
                    OffsetDateTime.now(clock)
            );
            publishTranscodeRequest(savedAudioMedia, inboundMessage, event.correlationId());
        } catch (RuntimeException exception) {
            recordFailure(inboundMessage, "Audio download failed: " + failureReason(exception));
            throw new WhatsappWorkerRetryableException(
                    inboundMessage.id(),
                    inboundMessage.providerMessageId(),
                    "Failed to process WhatsApp audio download worker event.",
                    exception
            );
        }
    }

    private DownloadedWhatsappAudio resolveAudio(WhatsappAudioPayload payload) {
        if (payload.rawContent() != null) {
            return new DownloadedWhatsappAudio(payload.mimeType(), payload.rawContent());
        }
        return downloadGateway.download(payload);
    }

    private void publishTranscodeRequest(
            WhatsappAudioMedia audioMedia,
            WhatsappInboundMessage inboundMessage,
            String correlationId
    ) {
        try {
            transcodeRequestedPublisher.publishRequested(new WhatsappAudioTranscodeRequestedEvent(
                    1,
                    "whatsapp.audio.transcode.requested.v1",
                    inboundMessage.id().toString(),
                    correlationId,
                    OffsetDateTime.now(clock),
                    inboundMessage.id(),
                    inboundMessage.providerMessageId(),
                    audioMedia.storagePath(),
                    audioMedia.mimeType()
            ));
        } catch (RuntimeException exception) {
            throw new WhatsappAudioTranscodePublishFailedException(
                    inboundMessage.id(),
                    inboundMessage.providerMessageId(),
                    exception
            );
        }
    }

    private void handleTextMessage(WhatsappMessageReceivedEvent event) {
        WhatsappInboundMessage inboundMessage = inboundMessageRepository.findByProviderMessageId(event.providerMessageId())
                .orElseThrow(() -> new IllegalStateException(
                        "Inbound message not found for provider message ID: " + event.providerMessageId()
                ));

        if (inboundMessage.status() == WhatsappProcessingStatus.TRANSCRIBED
                || transcriptRepository.findByInboundMessageId(inboundMessage.id()).isPresent()) {
            publishExtractionRequest(inboundMessage, event.correlationId());
            return;
        }

        String text = extractConversationText(inboundMessage.rawPayload());
        if (text == null || text.isBlank()) {
            recordFailure(inboundMessage, "Text message has no conversation content.");
            return;
        }

        transcriptRepository.save(WhatsappTranscript.transcribed(
                inboundMessage.id(),
                "text",
                "text",
                text,
                OffsetDateTime.now(clock)
        ));
        inboundMessageRepository.recordProcessingStep(
                inboundMessage.id(),
                WhatsappProcessingStatus.TRANSCRIBED,
                "Text message received: " + text,
                OffsetDateTime.now(clock)
        );
        publishExtractionRequest(inboundMessage, event.correlationId());
    }

    private String extractConversationText(String rawPayload) {
        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            String conversation = root.path("data").path("message").path("conversation").asText("");
            if (!conversation.isBlank()) return conversation;
            return root.path("data").path("message").path("extendedTextMessage").path("text").asText(null);
        } catch (Exception e) {
            return null;
        }
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

    private void recordFailure(WhatsappInboundMessage inboundMessage, String details) {
        inboundMessageRepository.recordProcessingStep(
                inboundMessage.id(),
                WhatsappProcessingStatus.FAILED,
                details,
                OffsetDateTime.now(clock)
        );
    }

    private String resolveMimeType(WhatsappAudioPayload payload, DownloadedWhatsappAudio downloadedAudio) {
        if (payload.mimeType() != null && !payload.mimeType().isBlank()) {
            return payload.mimeType();
        }
        if (downloadedAudio.contentType() != null && !downloadedAudio.contentType().isBlank()) {
            return downloadedAudio.contentType();
        }
        return "application/octet-stream";
    }

    private String failureReason(RuntimeException exception) {
        if (exception.getMessage() == null || exception.getMessage().isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return exception.getMessage();
    }
}
