package br.com.curralzap.backend.whatsapp.domain;

public enum WhatsappProcessingStatus {
    RECEIVED,
    MEDIA_DOWNLOADED,
    TRANSCODED,
    TRANSCRIBED,
    INTENT_EXTRACTED,
    VALIDATED,
    AWAITING_CONFIRMATION,
    APPLIED,
    REJECTED,
    FAILED,
    PENDING_FARM_SELECTION,
    FARM_SELECTED
}
