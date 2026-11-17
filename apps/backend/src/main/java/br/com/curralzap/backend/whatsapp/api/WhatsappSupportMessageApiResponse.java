package br.com.curralzap.backend.whatsapp.api;

import br.com.curralzap.backend.whatsapp.application.WhatsappSupportExtractedPayloadView;
import br.com.curralzap.backend.whatsapp.application.WhatsappSupportMessageDetailView;
import br.com.curralzap.backend.whatsapp.application.WhatsappSupportOutboundMessageView;
import br.com.curralzap.backend.whatsapp.application.WhatsappSupportProcessingStepView;
import br.com.curralzap.backend.whatsapp.application.WhatsappSupportRetryView;
import br.com.curralzap.backend.whatsapp.application.WhatsappSupportTranscriptView;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record WhatsappSupportMessageApiResponse(
        UUID id,
        String providerMessageId,
        String eventType,
        String phoneNumber,
        UUID farmId,
        UUID userId,
        String correlationId,
        String messageType,
        String rawPayload,
        String status,
        String failureReason,
        OffsetDateTime receivedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Retry retry,
        List<ProcessingStep> processingSteps,
        Transcript transcript,
        ExtractedPayload extractedPayload,
        List<OutboundMessage> outboundMessages
) {

    static WhatsappSupportMessageApiResponse from(WhatsappSupportMessageDetailView view) {
        return new WhatsappSupportMessageApiResponse(
                view.id(),
                view.providerMessageId(),
                view.eventType(),
                view.phoneNumber(),
                view.farmId(),
                view.userId(),
                view.correlationId(),
                view.messageType(),
                view.rawPayload(),
                view.status(),
                view.failureReason(),
                view.receivedAt(),
                view.createdAt(),
                view.updatedAt(),
                Retry.from(view.retry()),
                view.processingSteps().stream().map(ProcessingStep::from).toList(),
                Transcript.from(view.transcript()),
                ExtractedPayload.from(view.extractedPayload()),
                view.outboundMessages().stream().map(OutboundMessage::from).toList()
        );
    }

    record Retry(
            boolean supported,
            String action,
            String reason
    ) {

        static Retry from(WhatsappSupportRetryView view) {
            return new Retry(view.supported(), view.action(), view.reason());
        }
    }

    record ProcessingStep(
            UUID id,
            String status,
            String details,
            OffsetDateTime occurredAt
    ) {

        static ProcessingStep from(WhatsappSupportProcessingStepView view) {
            return new ProcessingStep(view.id(), view.status(), view.details(), view.occurredAt());
        }
    }

    record Transcript(
            UUID id,
            String provider,
            String providerModel,
            String transcript,
            OffsetDateTime transcribedAt
    ) {

        static Transcript from(WhatsappSupportTranscriptView view) {
            if (view == null) {
                return null;
            }
            return new Transcript(
                    view.id(),
                    view.provider(),
                    view.providerModel(),
                    view.transcript(),
                    view.transcribedAt()
            );
        }
    }

    record ExtractedPayload(
            UUID id,
            String provider,
            String providerModel,
            String intent,
            String structuredPayload,
            OffsetDateTime extractedAt
    ) {

        static ExtractedPayload from(WhatsappSupportExtractedPayloadView view) {
            if (view == null) {
                return null;
            }
            return new ExtractedPayload(
                    view.id(),
                    view.provider(),
                    view.providerModel(),
                    view.intent(),
                    view.structuredPayload(),
                    view.extractedAt()
            );
        }
    }

    record OutboundMessage(
            UUID id,
            String status,
            String payload,
            String providerMessageId,
            String failureReason,
            OffsetDateTime queuedAt,
            OffsetDateTime sentAt,
            OffsetDateTime updatedAt
    ) {

        static OutboundMessage from(WhatsappSupportOutboundMessageView view) {
            return new OutboundMessage(
                    view.id(),
                    view.status(),
                    view.payload(),
                    view.providerMessageId(),
                    view.failureReason(),
                    view.queuedAt(),
                    view.sentAt(),
                    view.updatedAt()
            );
        }
    }
}
