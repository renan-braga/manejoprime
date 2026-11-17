package br.com.curralzap.backend.whatsapp.application;

public interface WhatsappTranscriptExtractor {

    WhatsappTranscriptExtractionResult extract(WhatsappTranscriptExtractionRequest request);
}
