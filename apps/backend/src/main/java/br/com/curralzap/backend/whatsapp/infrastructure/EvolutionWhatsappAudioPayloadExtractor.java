package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.whatsapp.application.WhatsappAudioPayload;
import br.com.curralzap.backend.whatsapp.application.WhatsappAudioPayloadExtractor;
import br.com.curralzap.backend.whatsapp.domain.WhatsappInboundMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Optional;

@Component
class EvolutionWhatsappAudioPayloadExtractor implements WhatsappAudioPayloadExtractor {

    private static final Logger log = LoggerFactory.getLogger(EvolutionWhatsappAudioPayloadExtractor.class);

    private final ObjectMapper objectMapper;

    EvolutionWhatsappAudioPayloadExtractor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<WhatsappAudioPayload> extract(WhatsappInboundMessage message) {
        if (!"audioMessage".equals(message.messageType()) || message.rawPayload() == null || message.rawPayload().isBlank()) {
            return Optional.empty();
        }

        try {
            JsonNode root = objectMapper.readTree(message.rawPayload());
            JsonNode audioMessage = root.path("data").path("message").path("audioMessage");
            String mediaUrl = text(audioMessage, "url");
            if (mediaUrl != null) {
                String rawDataJson = root.path("data").toString();
                return Optional.of(new WhatsappAudioPayload(mediaUrl, mimeType(audioMessage), null, rawDataJson));
            }

            // Try data.message.base64 (Baileys mode) then data.base64 (Cloud API mode)
            String base64 = text(root.path("data").path("message"), "base64");
            if (base64 == null) {
                base64 = text(root.path("data"), "base64");
            }
            if (base64 != null) {
                byte[] content = Base64.getDecoder().decode(base64);
                return Optional.of(new WhatsappAudioPayload(null, mimeType(audioMessage), content));
            }

            log.warn("audioMessage payload has neither url nor base64 — data keys: {} — data.message keys: {} — data.message.audioMessage keys: {}",
                    fieldNames(root.path("data")),
                    fieldNames(root.path("data").path("message")),
                    fieldNames(audioMessage));
            return Optional.empty();
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private String fieldNames(JsonNode node) {
        if (node == null || node.isMissingNode()) return "(missing)";
        java.util.List<String> names = new java.util.ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names.toString();
    }

    private String mimeType(JsonNode audioMessage) {
        String mt = text(audioMessage, "mimetype");
        if (mt != null) {
            return mt;
        }
        return text(audioMessage, "mime_type");
    }

    private String text(JsonNode parent, String fieldName) {
        JsonNode field = parent.get(fieldName);
        if (field == null || !field.isTextual() || field.asText().isBlank()) {
            return null;
        }
        return field.asText();
    }
}
