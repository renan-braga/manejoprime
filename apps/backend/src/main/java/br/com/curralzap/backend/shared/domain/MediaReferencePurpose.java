package br.com.curralzap.backend.shared.domain;

import java.util.Arrays;

public enum MediaReferencePurpose {
    OCCURRENCE_PHOTO("occurrence_photo"),
    WHATSAPP_SOURCE_AUDIO("whatsapp_source_audio"),
    WHATSAPP_DOWNLOADED_AUDIO("whatsapp_downloaded_audio"),
    WHATSAPP_TRANSCODED_AUDIO("whatsapp_transcoded_audio");

    private final String databaseValue;

    MediaReferencePurpose(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String databaseValue() {
        return databaseValue;
    }

    public static MediaReferencePurpose fromDatabaseValue(String databaseValue) {
        return Arrays.stream(values())
                .filter(value -> value.databaseValue.equals(databaseValue))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported media reference purpose: " + databaseValue));
    }
}
