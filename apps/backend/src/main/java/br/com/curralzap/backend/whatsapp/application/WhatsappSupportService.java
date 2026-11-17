package br.com.curralzap.backend.whatsapp.application;

import br.com.curralzap.backend.auth.application.AuthTokenClaims;
import br.com.curralzap.backend.auth.application.AuthenticationRequiredException;
import br.com.curralzap.backend.auth.application.LoginUserPolicy;
import br.com.curralzap.backend.auth.application.RolePermissionSummary;
import br.com.curralzap.backend.users.domain.UserAccount;
import br.com.curralzap.backend.users.domain.UserAccountRepository;
import br.com.curralzap.backend.whatsapp.domain.WhatsappAudioMedia;
import br.com.curralzap.backend.whatsapp.domain.WhatsappExtractedPayload;
import br.com.curralzap.backend.whatsapp.domain.WhatsappInboundMessage;
import br.com.curralzap.backend.whatsapp.domain.WhatsappOutboundMessage;
import br.com.curralzap.backend.whatsapp.domain.WhatsappOutboundStatus;
import br.com.curralzap.backend.whatsapp.domain.WhatsappProcessingStatus;
import br.com.curralzap.backend.whatsapp.domain.WhatsappProcessingStep;
import br.com.curralzap.backend.whatsapp.domain.WhatsappTranscript;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class WhatsappSupportService {

    private static final String SUPPORT_PERMISSION = "whatsapp.support";
    private static final String RECEIVED_EVENT_TOPIC = "whatsapp.message.received";
    private static final String AUDIO_TRANSCODE_TOPIC = "whatsapp.audio.transcode.requested";
    private static final String AUDIO_TRANSCRIPTION_TOPIC = "whatsapp.audio.transcription.requested";
    private static final String TRANSCRIPT_EXTRACTION_TOPIC = "whatsapp.transcript.extraction.requested";
    private static final String PAYLOAD_VALIDATION_TOPIC = "whatsapp.payload.validation.requested";

    private final UserAccountRepository userAccountRepository;
    private final LoginUserPolicy loginUserPolicy;
    private final WhatsappSupportReadViewRepository supportReadViewRepository;
    private final WhatsappInboundMessageRepository inboundMessageRepository;
    private final WhatsappOutboundMessageRepository outboundMessageRepository;
    private final WhatsappAudioMediaRepository audioMediaRepository;
    private final WhatsappTranscriptRepository transcriptRepository;
    private final WhatsappExtractedPayloadRepository extractedPayloadRepository;
    private final WhatsappReceivedMessagePublisher receivedMessagePublisher;
    private final WhatsappAudioTranscodeRequestedPublisher audioTranscodeRequestedPublisher;
    private final WhatsappAudioTranscriptionRequestedPublisher audioTranscriptionRequestedPublisher;
    private final WhatsappTranscriptExtractionRequestedPublisher transcriptExtractionRequestedPublisher;
    private final WhatsappOutboundDeliveryRequestedPublisher outboundDeliveryRequestedPublisher;
    private final Clock clock;

    @Autowired
    public WhatsappSupportService(
            UserAccountRepository userAccountRepository,
            LoginUserPolicy loginUserPolicy,
            WhatsappSupportReadViewRepository supportReadViewRepository,
            WhatsappInboundMessageRepository inboundMessageRepository,
            WhatsappOutboundMessageRepository outboundMessageRepository,
            WhatsappAudioMediaRepository audioMediaRepository,
            WhatsappTranscriptRepository transcriptRepository,
            WhatsappExtractedPayloadRepository extractedPayloadRepository,
            WhatsappReceivedMessagePublisher receivedMessagePublisher,
            WhatsappAudioTranscodeRequestedPublisher audioTranscodeRequestedPublisher,
            WhatsappAudioTranscriptionRequestedPublisher audioTranscriptionRequestedPublisher,
            WhatsappTranscriptExtractionRequestedPublisher transcriptExtractionRequestedPublisher,
            WhatsappOutboundDeliveryRequestedPublisher outboundDeliveryRequestedPublisher
    ) {
        this(
                userAccountRepository,
                loginUserPolicy,
                supportReadViewRepository,
                inboundMessageRepository,
                outboundMessageRepository,
                audioMediaRepository,
                transcriptRepository,
                extractedPayloadRepository,
                receivedMessagePublisher,
                audioTranscodeRequestedPublisher,
                audioTranscriptionRequestedPublisher,
                transcriptExtractionRequestedPublisher,
                outboundDeliveryRequestedPublisher,
                Clock.systemUTC()
        );
    }

    WhatsappSupportService(
            UserAccountRepository userAccountRepository,
            LoginUserPolicy loginUserPolicy,
            WhatsappSupportReadViewRepository supportReadViewRepository,
            WhatsappInboundMessageRepository inboundMessageRepository,
            WhatsappOutboundMessageRepository outboundMessageRepository,
            WhatsappAudioMediaRepository audioMediaRepository,
            WhatsappTranscriptRepository transcriptRepository,
            WhatsappExtractedPayloadRepository extractedPayloadRepository,
            WhatsappReceivedMessagePublisher receivedMessagePublisher,
            WhatsappAudioTranscodeRequestedPublisher audioTranscodeRequestedPublisher,
            WhatsappAudioTranscriptionRequestedPublisher audioTranscriptionRequestedPublisher,
            WhatsappTranscriptExtractionRequestedPublisher transcriptExtractionRequestedPublisher,
            WhatsappOutboundDeliveryRequestedPublisher outboundDeliveryRequestedPublisher,
            Clock clock
    ) {
        this.userAccountRepository = userAccountRepository;
        this.loginUserPolicy = loginUserPolicy;
        this.supportReadViewRepository = supportReadViewRepository;
        this.inboundMessageRepository = inboundMessageRepository;
        this.outboundMessageRepository = outboundMessageRepository;
        this.audioMediaRepository = audioMediaRepository;
        this.transcriptRepository = transcriptRepository;
        this.extractedPayloadRepository = extractedPayloadRepository;
        this.receivedMessagePublisher = receivedMessagePublisher;
        this.audioTranscodeRequestedPublisher = audioTranscodeRequestedPublisher;
        this.audioTranscriptionRequestedPublisher = audioTranscriptionRequestedPublisher;
        this.transcriptExtractionRequestedPublisher = transcriptExtractionRequestedPublisher;
        this.outboundDeliveryRequestedPublisher = outboundDeliveryRequestedPublisher;
        this.clock = clock;
    }

    public WhatsappSupportMessageListView list(AuthTokenClaims claims, WhatsappSupportMessageQuery query) {
        requirePermission(claims);
        return supportReadViewRepository.listFor(claims.farmId(), claims.userId(), query);
    }

    public WhatsappSupportMessageDetailView detail(AuthTokenClaims claims, UUID inboundMessageId) {
        requirePermission(claims);

        WhatsappInboundMessage inboundMessage = inboundMessageRepository.findById(inboundMessageId)
                .filter(message -> claims.farmId().equals(message.farmId()))
                .orElseThrow(WhatsappSupportMessageNotFoundException::new);

        List<WhatsappProcessingStep> processingSteps = inboundMessageRepository.findProcessingSteps(inboundMessageId);
        Optional<WhatsappTranscript> transcript = transcriptRepository.findByInboundMessageId(inboundMessageId);
        Optional<WhatsappExtractedPayload> extractedPayload = extractedPayloadRepository.findByInboundMessageId(inboundMessageId);
        List<WhatsappOutboundMessage> outboundMessages = outboundMessageRepository.findByInboundMessageIdOrderByCreatedAtAsc(
                inboundMessageId
        );
        RetryPlan retryPlan = retryPlan(inboundMessage, processingSteps, outboundMessages);

        return new WhatsappSupportMessageDetailView(
                inboundMessage.id(),
                inboundMessage.providerMessageId(),
                inboundMessage.eventType(),
                inboundMessage.phoneNumber(),
                inboundMessage.farmId(),
                inboundMessage.userId(),
                inboundMessage.correlationId(),
                inboundMessage.messageType(),
                inboundMessage.rawPayload(),
                apiStatus(inboundMessage.status()),
                inboundMessage.failureReason(),
                inboundMessage.receivedAt(),
                inboundMessage.createdAt(),
                inboundMessage.updatedAt(),
                new WhatsappSupportRetryView(retryPlan.supported(), retryPlan.action(), retryPlan.reason()),
                processingSteps.stream().map(this::toProcessingStepView).toList(),
                transcript.map(this::toTranscriptView).orElse(null),
                extractedPayload.map(this::toExtractedPayloadView).orElse(null),
                outboundMessages.stream().map(this::toOutboundMessageView).toList()
        );
    }

    public WhatsappSupportRetryView retry(AuthTokenClaims claims, UUID inboundMessageId) {
        requirePermission(claims);

        WhatsappInboundMessage inboundMessage = inboundMessageRepository.findById(inboundMessageId)
                .filter(message -> claims.farmId().equals(message.farmId()))
                .orElseThrow(WhatsappSupportMessageNotFoundException::new);

        List<WhatsappProcessingStep> processingSteps = inboundMessageRepository.findProcessingSteps(inboundMessageId);
        List<WhatsappOutboundMessage> outboundMessages = outboundMessageRepository.findByInboundMessageIdOrderByCreatedAtAsc(
                inboundMessageId
        );
        RetryPlan retryPlan = retryPlan(inboundMessage, processingSteps, outboundMessages);
        if (!retryPlan.supported()) {
            throw new WhatsappSupportRetryNotAllowedException(retryPlan.reason());
        }

        retryPlan.publisher().publish();
        return new WhatsappSupportRetryView(true, retryPlan.action(), retryPlan.reason());
    }

    private WhatsappSupportProcessingStepView toProcessingStepView(WhatsappProcessingStep step) {
        return new WhatsappSupportProcessingStepView(
                step.id(),
                apiStatus(step.status()),
                step.details(),
                step.occurredAt()
        );
    }

    private WhatsappSupportTranscriptView toTranscriptView(WhatsappTranscript transcript) {
        return new WhatsappSupportTranscriptView(
                transcript.id(),
                transcript.provider(),
                transcript.providerModel(),
                transcript.transcript(),
                transcript.transcribedAt()
        );
    }

    private WhatsappSupportExtractedPayloadView toExtractedPayloadView(WhatsappExtractedPayload extractedPayload) {
        return new WhatsappSupportExtractedPayloadView(
                extractedPayload.id(),
                extractedPayload.provider(),
                extractedPayload.providerModel(),
                extractedPayload.intent(),
                extractedPayload.structuredPayload(),
                extractedPayload.extractedAt()
        );
    }

    private WhatsappSupportOutboundMessageView toOutboundMessageView(WhatsappOutboundMessage outboundMessage) {
        return new WhatsappSupportOutboundMessageView(
                outboundMessage.id(),
                apiStatus(outboundMessage.status()),
                outboundMessage.payload(),
                outboundMessage.providerMessageId(),
                outboundMessage.failureReason(),
                outboundMessage.queuedAt(),
                outboundMessage.sentAt(),
                outboundMessage.updatedAt()
        );
    }

    private RetryPlan retryPlan(
            WhatsappInboundMessage inboundMessage,
            List<WhatsappProcessingStep> processingSteps,
            List<WhatsappOutboundMessage> outboundMessages
    ) {
        Optional<WhatsappOutboundMessage> latestOutbound = latestOutboundMessage(outboundMessages);
        if (latestOutbound.isPresent() && latestOutbound.get().status() == WhatsappOutboundStatus.FAILED) {
            Optional<WhatsappProcessingStatus> outboundStatus = latestOutboundBusinessStatus(processingSteps);
            if (outboundStatus.isPresent()) {
                return outboundDeliveryRetryPlan(inboundMessage, outboundStatus.get());
            }
        }

        Optional<WhatsappProcessingStep> latestFailed = latestFailedStep(processingSteps);
        if (latestFailed.isEmpty()) {
            return RetryPlan.unsupported("Only failed or outbound-delivery-pending messages can be retried.");
        }

        String details = latestFailed.get().details() == null ? "" : latestFailed.get().details();
        if (details.startsWith("Received event publish failed:")
                || details.startsWith("Audio download failed:")
                || details.contains(RECEIVED_EVENT_TOPIC)) {
            return receivedRetryPlan(inboundMessage);
        }
        if (details.startsWith("Audio transcode failed:")
                || details.contains(AUDIO_TRANSCODE_TOPIC)) {
            return transcodeRetryPlan(inboundMessage);
        }
        if (details.startsWith("Audio transcription failed:")
                || details.contains(AUDIO_TRANSCRIPTION_TOPIC)) {
            return transcriptionRetryPlan(inboundMessage);
        }
        if (details.startsWith("Transcript extraction failed:")
                || details.contains(TRANSCRIPT_EXTRACTION_TOPIC)) {
            return extractionRetryPlan(inboundMessage);
        }
        if (details.contains(PAYLOAD_VALIDATION_TOPIC)) {
            Optional<WhatsappProcessingStatus> outboundStatus = latestOutboundBusinessStatus(processingSteps);
            if (outboundStatus.isPresent()) {
                return outboundDeliveryRetryPlan(inboundMessage, outboundStatus.get());
            }
        }

        return RetryPlan.unsupported("No supported retry path is available for this WhatsApp message.");
    }

    private RetryPlan receivedRetryPlan(WhatsappInboundMessage inboundMessage) {
        return new RetryPlan(
                true,
                "message_received",
                "Retry will republish the initial WhatsApp received event.",
                () -> receivedMessagePublisher.publishReceived(new WhatsappMessageReceivedEvent(
                        1,
                        "whatsapp.message.received.v1",
                        inboundMessage.id().toString(),
                        correlationId(inboundMessage),
                        inboundMessage.receivedAt(),
                        inboundMessage.id(),
                        inboundMessage.providerMessageId(),
                        inboundMessage.farmId(),
                        inboundMessage.userId(),
                        inboundMessage.phoneNumber(),
                        inboundMessage.messageType(),
                        inboundMessage.status()
                ))
        );
    }

    private RetryPlan transcodeRetryPlan(WhatsappInboundMessage inboundMessage) {
        WhatsappAudioMedia audioMedia = audioMediaRepository.findByInboundMessageId(inboundMessage.id())
                .orElseThrow(() -> new WhatsappSupportRetryNotAllowedException(
                        "Downloaded audio is missing for this WhatsApp message."
                ));
        return new RetryPlan(
                true,
                "audio_transcode_requested",
                "Retry will republish the audio transcode worker event.",
                () -> audioTranscodeRequestedPublisher.publishRequested(new WhatsappAudioTranscodeRequestedEvent(
                        1,
                        "whatsapp.audio.transcode.requested.v1",
                        inboundMessage.id().toString(),
                        correlationId(inboundMessage),
                        OffsetDateTime.now(clock),
                        inboundMessage.id(),
                        inboundMessage.providerMessageId(),
                        audioMedia.storagePath(),
                        audioMedia.mimeType()
                ))
        );
    }

    private RetryPlan transcriptionRetryPlan(WhatsappInboundMessage inboundMessage) {
        WhatsappAudioMedia audioMedia = audioMediaRepository.findByInboundMessageId(inboundMessage.id())
                .orElseThrow(() -> new WhatsappSupportRetryNotAllowedException(
                        "Transcoded audio is missing for this WhatsApp message."
                ));
        if (audioMedia.transcodedStoragePath() == null || audioMedia.transcodedMimeType() == null) {
            throw new WhatsappSupportRetryNotAllowedException(
                    "Transcoded audio is missing for this WhatsApp message."
            );
        }
        return new RetryPlan(
                true,
                "audio_transcription_requested",
                "Retry will republish the audio transcription worker event.",
                () -> audioTranscriptionRequestedPublisher.publishRequested(new WhatsappAudioTranscriptionRequestedEvent(
                        1,
                        "whatsapp.audio.transcription.requested.v1",
                        inboundMessage.id().toString(),
                        correlationId(inboundMessage),
                        OffsetDateTime.now(clock),
                        inboundMessage.id(),
                        inboundMessage.providerMessageId(),
                        audioMedia.transcodedStoragePath(),
                        audioMedia.transcodedMimeType()
                ))
        );
    }

    private RetryPlan extractionRetryPlan(WhatsappInboundMessage inboundMessage) {
        return new RetryPlan(
                true,
                "transcript_extraction_requested",
                "Retry will republish the transcript extraction worker event.",
                () -> transcriptExtractionRequestedPublisher.publishRequested(new WhatsappTranscriptExtractionRequestedEvent(
                        1,
                        "whatsapp.transcript.extraction.requested.v1",
                        inboundMessage.id().toString(),
                        correlationId(inboundMessage),
                        OffsetDateTime.now(clock),
                        inboundMessage.id(),
                        inboundMessage.providerMessageId()
                ))
        );
    }

    private RetryPlan outboundDeliveryRetryPlan(
            WhatsappInboundMessage inboundMessage,
            WhatsappProcessingStatus outboundStatus
    ) {
        return new RetryPlan(
                true,
                "outbound_delivery_requested",
                "Retry will republish the outbound delivery worker event.",
                () -> outboundDeliveryRequestedPublisher.publishRequested(new WhatsappOutboundDeliveryRequestedEvent(
                        1,
                        "whatsapp.outbound.delivery.requested.v1",
                        UUID.randomUUID().toString(),
                        correlationId(inboundMessage),
                        OffsetDateTime.now(clock),
                        inboundMessage.id(),
                        inboundMessage.providerMessageId(),
                        outboundStatus
                ))
        );
    }

    private Optional<WhatsappOutboundMessage> latestOutboundMessage(List<WhatsappOutboundMessage> outboundMessages) {
        return outboundMessages.stream()
                .max(Comparator.comparing(WhatsappOutboundMessage::createdAt));
    }

    private Optional<WhatsappProcessingStep> latestFailedStep(List<WhatsappProcessingStep> processingSteps) {
        return processingSteps.stream()
                .filter(step -> step.status() == WhatsappProcessingStatus.FAILED)
                .max(Comparator.comparing(WhatsappProcessingStep::occurredAt));
    }

    private Optional<WhatsappProcessingStatus> latestOutboundBusinessStatus(List<WhatsappProcessingStep> processingSteps) {
        return processingSteps.stream()
                .filter(step -> step.status() == WhatsappProcessingStatus.AWAITING_CONFIRMATION
                        || step.status() == WhatsappProcessingStatus.REJECTED)
                .max(Comparator.comparing(WhatsappProcessingStep::occurredAt))
                .map(WhatsappProcessingStep::status);
    }

    private String correlationId(WhatsappInboundMessage inboundMessage) {
        if (inboundMessage.correlationId() != null && !inboundMessage.correlationId().isBlank()) {
            return inboundMessage.correlationId();
        }
        return UUID.randomUUID().toString();
    }

    private void requirePermission(AuthTokenClaims claims) {
        UserAccount activeUser = userAccountRepository.findById(claims.userId())
                .orElseThrow(AuthenticationRequiredException::new);
        activeUser = loginUserPolicy.requireActive(activeUser);
        if (!RolePermissionSummary.permissionsFor(activeUser.role()).contains(SUPPORT_PERMISSION)) {
            throw new WhatsappSupportForbiddenException();
        }
    }

    private String apiStatus(WhatsappProcessingStatus status) {
        return status.name().toLowerCase();
    }

    private String apiStatus(WhatsappOutboundStatus status) {
        return status.name().toLowerCase();
    }

    private record RetryPlan(
            boolean supported,
            String action,
            String reason,
            RetryPublisher publisher
    ) {

        static RetryPlan unsupported(String reason) {
            return new RetryPlan(false, null, reason, () -> {
            });
        }
    }

    @FunctionalInterface
    private interface RetryPublisher {
        void publish();
    }
}
