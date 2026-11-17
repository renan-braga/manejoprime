package br.com.curralzap.backend.whatsapp.application;

import br.com.curralzap.backend.whatsapp.domain.WhatsappAudioMedia;
import br.com.curralzap.backend.whatsapp.domain.WhatsappInboundMessage;
import br.com.curralzap.backend.whatsapp.domain.WhatsappProcessingStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;

@Service
public class WhatsappAudioTranscodeService {

    private final WhatsappInboundMessageRepository inboundMessageRepository;
    private final WhatsappAudioMediaRepository audioMediaRepository;
    private final WhatsappAudioStorage audioStorage;
    private final WhatsappAudioTranscoder audioTranscoder;
    private final WhatsappAudioTranscriptionRequestedPublisher transcriptionRequestedPublisher;
    private final Clock clock;

    @Autowired
    public WhatsappAudioTranscodeService(
            WhatsappInboundMessageRepository inboundMessageRepository,
            WhatsappAudioMediaRepository audioMediaRepository,
            WhatsappAudioStorage audioStorage,
            WhatsappAudioTranscoder audioTranscoder,
            WhatsappAudioTranscriptionRequestedPublisher transcriptionRequestedPublisher
    ) {
        this(
                inboundMessageRepository,
                audioMediaRepository,
                audioStorage,
                audioTranscoder,
                transcriptionRequestedPublisher,
                Clock.systemUTC()
        );
    }

    WhatsappAudioTranscodeService(
            WhatsappInboundMessageRepository inboundMessageRepository,
            WhatsappAudioMediaRepository audioMediaRepository,
            WhatsappAudioStorage audioStorage,
            WhatsappAudioTranscoder audioTranscoder,
            WhatsappAudioTranscriptionRequestedPublisher transcriptionRequestedPublisher,
            Clock clock
    ) {
        this.inboundMessageRepository = inboundMessageRepository;
        this.audioMediaRepository = audioMediaRepository;
        this.audioStorage = audioStorage;
        this.audioTranscoder = audioTranscoder;
        this.transcriptionRequestedPublisher = transcriptionRequestedPublisher;
        this.clock = clock;
    }

    @Transactional
    public void handleRequestedTranscode(WhatsappAudioTranscodeRequestedEvent event) {
        WhatsappInboundMessage inboundMessage = inboundMessageRepository.findByProviderMessageId(event.providerMessageId())
                .orElseThrow(() -> new IllegalStateException(
                        "Inbound message not found for provider message ID: " + event.providerMessageId()
                ));
        WhatsappAudioMedia audioMedia = audioMediaRepository.findByInboundMessageId(event.inboundMessageId())
                .orElseThrow(() -> new WhatsappWorkerRetryableException(
                        event.inboundMessageId(),
                        event.providerMessageId(),
                        "Downloaded audio media not yet visible for inbound message ID: " + event.inboundMessageId() + " — retrying after download transaction commit",
                        new IllegalStateException("Downloaded audio media not found")
                ));

        if (inboundMessage.status() == WhatsappProcessingStatus.TRANSCRIBED) {
            return;
        }

        if (audioMedia.transcodedAt() != null || inboundMessage.status() == WhatsappProcessingStatus.TRANSCODED) {
            publishTranscriptionRequest(audioMedia, inboundMessage, event.correlationId());
            return;
        }

        try {
            byte[] downloadedContent = audioStorage.load(audioMedia.storagePath());
            TranscodedWhatsappAudio transcodedAudio = audioTranscoder.transcode(new WhatsappAudioTranscodeRequest(
                    inboundMessage.providerMessageId(),
                    audioMedia.mimeType(),
                    downloadedContent
            ));
            String storagePath = audioStorage.store(new StoreWhatsappAudioRequest(
                    inboundMessage.id(),
                    inboundMessage.providerMessageId(),
                    inboundMessage.providerMessageId() + "-transcoded",
                    transcodedAudio.mimeType(),
                    transcodedAudio.content()
            ));

            audioMediaRepository.save(audioMedia.markTranscoded(
                    transcodedAudio.mimeType(),
                    storagePath,
                    OffsetDateTime.now(clock)
            ));
            inboundMessageRepository.recordProcessingStep(
                    inboundMessage.id(),
                    WhatsappProcessingStatus.TRANSCODED,
                    "Audio transcoded to " + storagePath + ".",
                    OffsetDateTime.now(clock)
            );
        } catch (RuntimeException exception) {
            inboundMessageRepository.recordProcessingStep(
                    inboundMessage.id(),
                    WhatsappProcessingStatus.FAILED,
                    "Audio transcode failed: " + failureReason(exception),
                    OffsetDateTime.now(clock)
            );
            throw new WhatsappWorkerRetryableException(
                    inboundMessage.id(),
                    inboundMessage.providerMessageId(),
                    "Failed to process WhatsApp audio transcode worker event.",
                    exception
            );
        }

        publishTranscriptionRequest(
                audioMediaRepository.findByInboundMessageId(event.inboundMessageId()).orElseThrow(),
                inboundMessageRepository.findByProviderMessageId(event.providerMessageId()).orElseThrow(),
                event.correlationId()
        );
    }

    private String failureReason(RuntimeException exception) {
        if (exception.getMessage() == null || exception.getMessage().isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return exception.getMessage();
    }

    private void publishTranscriptionRequest(
            WhatsappAudioMedia audioMedia,
            WhatsappInboundMessage inboundMessage,
            String correlationId
    ) {
        if (audioMedia.transcodedStoragePath() == null || audioMedia.transcodedMimeType() == null) {
            throw new IllegalStateException("Transcoded audio is not available.");
        }

        try {
            transcriptionRequestedPublisher.publishRequested(new WhatsappAudioTranscriptionRequestedEvent(
                    1,
                    "whatsapp.audio.transcription.requested.v1",
                    inboundMessage.id().toString(),
                    correlationId,
                    OffsetDateTime.now(clock),
                    inboundMessage.id(),
                    inboundMessage.providerMessageId(),
                    audioMedia.transcodedStoragePath(),
                    audioMedia.transcodedMimeType()
            ));
        } catch (RuntimeException exception) {
            throw new WhatsappAudioTranscriptionPublishFailedException(
                    inboundMessage.id(),
                    inboundMessage.providerMessageId(),
                    exception
            );
        }
    }
}
