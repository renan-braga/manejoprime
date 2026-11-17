package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.whatsapp.domain.WhatsappInboundMessage;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EvolutionWhatsappAudioPayloadExtractorTest {

    private final EvolutionWhatsappAudioPayloadExtractor extractor =
            new EvolutionWhatsappAudioPayloadExtractor(new com.fasterxml.jackson.databind.ObjectMapper());

    // ─── URL-based audio ──────────────────────────────────────────────────────

    @Test
    void extractsDownloadableAudioPayloadFromStoredWebhookJson() {
        WhatsappInboundMessage message = audioMessage("wamid.audio-extract", """
                {
                  "event": "messages.upsert",
                  "data": {
                    "key": {"id": "wamid.audio-extract", "remoteJid": "5511999990001@s.whatsapp.net", "fromMe": false},
                    "messageType": "audioMessage",
                    "message": {
                      "audioMessage": {
                        "url": "https://media.example/audio.ogg",
                        "mimetype": "audio/ogg"
                      }
                    }
                  }
                }
                """);

        assertThat(extractor.extract(message))
                .get()
                .satisfies(payload -> {
                    assertThat(payload.mediaUrl()).isEqualTo("https://media.example/audio.ogg");
                    assertThat(payload.mimeType()).isEqualTo("audio/ogg");
                    assertThat(payload.rawContent()).isNull();
                    assertThat(payload.rawDataJson()).isNotNull().contains("wamid.audio-extract");
                });
    }

    @Test
    void extractsMimeTypeWithUnderscoreFieldName() {
        WhatsappInboundMessage message = audioMessage("wamid.audio-mime-underscore", """
                {
                  "event": "messages.upsert",
                  "data": {
                    "key": {"id": "wamid.audio-mime-underscore", "remoteJid": "5511999990001@s.whatsapp.net", "fromMe": false},
                    "messageType": "audioMessage",
                    "message": {
                      "audioMessage": {
                        "url": "https://media.example/audio.ogg",
                        "mime_type": "audio/ogg; codecs=opus"
                      }
                    }
                  }
                }
                """);

        assertThat(extractor.extract(message))
                .get()
                .satisfies(payload -> {
                    assertThat(payload.mediaUrl()).isEqualTo("https://media.example/audio.ogg");
                    assertThat(payload.mimeType()).isEqualTo("audio/ogg; codecs=opus");
                    assertThat(payload.rawDataJson()).isNotNull();
                });
    }

    // ─── Base64 audio ─────────────────────────────────────────────────────────

    @Test
    void extractsBase64ContentAtDataMessageBase64() {
        byte[] audioBytes = "fake-ogg-audio-content".getBytes();
        String base64 = Base64.getEncoder().encodeToString(audioBytes);
        WhatsappInboundMessage message = audioMessage("wamid.audio-base64", """
                {
                  "event": "messages.upsert",
                  "data": {
                    "key": {"id": "wamid.audio-base64", "remoteJid": "5511900000000@s.whatsapp.net", "fromMe": false},
                    "messageType": "audioMessage",
                    "message": {
                      "audioMessage": {
                        "mime_type": "audio/ogg; codecs=opus",
                        "voice": true
                      },
                      "base64": "%s"
                    }
                  }
                }
                """.formatted(base64));

        assertThat(extractor.extract(message))
                .get()
                .satisfies(payload -> {
                    assertThat(payload.mediaUrl()).isNull();
                    assertThat(payload.mimeType()).isEqualTo("audio/ogg; codecs=opus");
                    assertThat(payload.rawContent()).isEqualTo(audioBytes);
                    assertThat(payload.rawDataJson()).isNull();
                });
    }

    @Test
    void extractsBase64ContentAtDataBase64WhenMessageBase64IsAbsent() {
        // Evolution API Cloud API mode puts base64 one level up: data.base64 not data.message.base64
        byte[] audioBytes = "fake-ogg-ptt-content".getBytes();
        String base64 = Base64.getEncoder().encodeToString(audioBytes);
        WhatsappInboundMessage message = audioMessage("wamid.audio-data-base64", """
                {
                  "event": "messages.upsert",
                  "data": {
                    "key": {"id": "wamid.audio-data-base64", "remoteJid": "5511900000000@s.whatsapp.net", "fromMe": false},
                    "messageType": "audioMessage",
                    "message": {
                      "audioMessage": {
                        "mimetype": "audio/ogg; codecs=opus",
                        "ptt": true,
                        "seconds": 5
                      }
                    },
                    "base64": "%s"
                  }
                }
                """.formatted(base64));

        assertThat(extractor.extract(message))
                .get()
                .satisfies(payload -> {
                    assertThat(payload.mediaUrl()).isNull();
                    assertThat(payload.mimeType()).isEqualTo("audio/ogg; codecs=opus");
                    assertThat(payload.rawContent()).isEqualTo(audioBytes);
                    assertThat(payload.rawDataJson()).isNull();
                });
    }

    @Test
    void prefersDataMessageBase64OverDataBase64WhenBothPresent() {
        byte[] messageBytes = "message-level-base64".getBytes();
        byte[] dataBytes = "data-level-base64".getBytes();
        String messageBase64 = Base64.getEncoder().encodeToString(messageBytes);
        String dataBase64 = Base64.getEncoder().encodeToString(dataBytes);
        WhatsappInboundMessage message = audioMessage("wamid.audio-both-base64", """
                {
                  "event": "messages.upsert",
                  "data": {
                    "key": {"id": "wamid.audio-both-base64", "remoteJid": "5511900000000@s.whatsapp.net", "fromMe": false},
                    "messageType": "audioMessage",
                    "message": {
                      "audioMessage": {"mimetype": "audio/ogg"},
                      "base64": "%s"
                    },
                    "base64": "%s"
                  }
                }
                """.formatted(messageBase64, dataBase64));

        assertThat(extractor.extract(message))
                .get()
                .satisfies(payload -> assertThat(payload.rawContent()).isEqualTo(messageBytes));
    }

    // ─── Empty / missing content ───────────────────────────────────────────────

    @Test
    void returnsEmptyWhenAudioMessageHasNeitherUrlNorBase64() {
        WhatsappInboundMessage message = audioMessage("wamid.audio-extract-missing", """
                {
                  "event": "messages.upsert",
                  "data": {
                    "key": {"id": "wamid.audio-extract-missing", "remoteJid": "5511999990001@s.whatsapp.net", "fromMe": false},
                    "messageType": "audioMessage",
                    "message": {
                      "audioMessage": {
                        "mimetype": "audio/ogg"
                      }
                    }
                  }
                }
                """);

        assertThat(extractor.extract(message)).isEmpty();
    }

    @Test
    void returnsEmptyWhenAudioMessageUrlIsBlank() {
        // Evolution API Cloud mode may set url to "" when the signed URL is not available
        WhatsappInboundMessage message = audioMessage("wamid.audio-blank-url", """
                {
                  "event": "messages.upsert",
                  "data": {
                    "key": {"id": "wamid.audio-blank-url", "remoteJid": "5511999990001@s.whatsapp.net", "fromMe": false},
                    "messageType": "audioMessage",
                    "message": {
                      "audioMessage": {
                        "url": "",
                        "mimetype": "audio/ogg; codecs=opus",
                        "ptt": true,
                        "seconds": 5,
                        "mediaKey": "somekey"
                      }
                    }
                  }
                }
                """);

        assertThat(extractor.extract(message)).isEmpty();
    }

    @Test
    void returnsEmptyForNonAudioMessageType() {
        WhatsappInboundMessage message = new WhatsappInboundMessage(
                null, "wamid.text", "messages.upsert", "5511999990001",
                null, null, null,
                "conversation",
                """
                        {"event":"messages.upsert","data":{"key":{"id":"wamid.text","remoteJid":"5511999990001@s.whatsapp.net","fromMe":false},"messageType":"conversation","message":{"conversation":"oi"}}}
                        """,
                null, null,
                OffsetDateTime.parse("2026-04-04T11:09:00Z"),
                OffsetDateTime.parse("2026-04-04T11:09:00Z"),
                OffsetDateTime.parse("2026-04-04T11:09:00Z")
        );

        assertThat(extractor.extract(message)).isEmpty();
    }

    @Test
    void returnsEmptyWhenRawPayloadIsBlank() {
        WhatsappInboundMessage message = new WhatsappInboundMessage(
                null, "wamid.blank", "messages.upsert", "5511999990001",
                null, null, null,
                "audioMessage",
                "   ",
                null, null,
                OffsetDateTime.parse("2026-04-04T11:10:00Z"),
                OffsetDateTime.parse("2026-04-04T11:10:00Z"),
                OffsetDateTime.parse("2026-04-04T11:10:00Z")
        );

        assertThat(extractor.extract(message)).isEmpty();
    }

    // ─── helper ───────────────────────────────────────────────────────────────

    private WhatsappInboundMessage audioMessage(String providerMessageId, String rawPayload) {
        return WhatsappInboundMessage.receive(
                providerMessageId,
                "messages.upsert",
                "5511900000000",
                null,
                null,
                "audioMessage",
                rawPayload,
                OffsetDateTime.parse("2026-04-04T11:05:00Z")
        );
    }
}
