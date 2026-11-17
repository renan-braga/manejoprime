package br.com.curralzap.backend.shared.domain;

import java.util.Arrays;

public enum MediaReferenceOwnerType {
    OCCURRENCE("occurrence"),
    WHATSAPP_AUDIO_MEDIA("whatsapp_audio_media");

    private final String databaseValue;

    MediaReferenceOwnerType(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String databaseValue() {
        return databaseValue;
    }

    public static MediaReferenceOwnerType fromDatabaseValue(String databaseValue) {
        return Arrays.stream(values())
                .filter(value -> value.databaseValue.equals(databaseValue))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported media reference owner type: " + databaseValue));
    }
}
