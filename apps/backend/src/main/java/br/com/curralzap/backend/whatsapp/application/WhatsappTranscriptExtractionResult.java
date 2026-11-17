package br.com.curralzap.backend.whatsapp.application;

import java.util.Map;

public record WhatsappTranscriptExtractionResult(
        String provider,
        String providerModel,
        String intent,
        Map<String, Object> fields,
        Double confidence,
        String reason
) {

    public static WhatsappTranscriptExtractionResult supported(
            String provider,
            String providerModel,
            String intent,
            Map<String, Object> fields,
            double confidence
    ) {
        return new WhatsappTranscriptExtractionResult(
                provider,
                providerModel,
                intent,
                fields == null ? Map.of() : Map.copyOf(fields),
                confidence,
                null
        );
    }

    public static WhatsappTranscriptExtractionResult unknown(
            String provider,
            String providerModel,
            String reason,
            double confidence
    ) {
        return new WhatsappTranscriptExtractionResult(provider, providerModel, "unknown", Map.of(), confidence, reason);
    }

    public boolean isUnknown() {
        return "unknown".equals(intent);
    }
}
