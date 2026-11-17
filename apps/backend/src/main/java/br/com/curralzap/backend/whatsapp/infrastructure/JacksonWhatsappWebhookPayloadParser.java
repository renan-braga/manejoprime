package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.whatsapp.application.InvalidWhatsappWebhookPayloadException;
import br.com.curralzap.backend.whatsapp.application.WhatsappWebhookIntakeCommand;
import br.com.curralzap.backend.whatsapp.application.WhatsappWebhookPayloadParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
class JacksonWhatsappWebhookPayloadParser implements WhatsappWebhookPayloadParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(JacksonWhatsappWebhookPayloadParser.class);

    private final ObjectMapper objectMapper;

    JacksonWhatsappWebhookPayloadParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public WhatsappWebhookIntakeCommand parse(String rawPayload) {
        JsonNode root = parseJson(rawPayload);
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        String eventType = requiredText(root, "event", "event", "Event type is required.", fieldErrors);
        if (eventType != null && !"messages.upsert".equals(eventType)) {
            fieldErrors.put("event", "Only messages.upsert events are supported.");
        }

        JsonNode data = objectNode(root, "data", "Webhook data is required.", fieldErrors);
        JsonNode key = objectNode(data, "key", "Webhook message key is required.", fieldErrors);
        String providerMessageId = requiredText(key, "id", "data.key.id", "Provider message ID is required.", fieldErrors);
        String remoteJid = requiredText(key, "remoteJid", "data.key.remoteJid", "Sender JID is required.", fieldErrors);
        Boolean fromMe = requiredBoolean(key, "fromMe", "data.key.fromMe", "fromMe flag is required.", fieldErrors);
        if (Boolean.TRUE.equals(fromMe)) {
            fieldErrors.put("data.key.fromMe", "Webhook must describe an inbound message.");
        }

        String messageType = requiredText(data, "messageType", "data.messageType", "Message type is required.", fieldErrors);

        if (!fieldErrors.isEmpty()) {
            throw new InvalidWhatsappWebhookPayloadException(fieldErrors);
        }

        String selectedButtonId = extractSelectedButtonId(data, messageType);
        LOGGER.info("Parsed webhook eventType={} messageType={} selectedButtonId={}", eventType, messageType, selectedButtonId);

        return new WhatsappWebhookIntakeCommand(
                eventType,
                providerMessageId,
                remoteJid,
                messageType,
                rawPayload,
                selectedButtonId
        );
    }

    private String extractSelectedButtonId(JsonNode data, String messageType) {
        if (messageType == null) {
            return null;
        }
        JsonNode message = data.path("message");
        if (message.isMissingNode() || message.isNull()) {
            return null;
        }
        if ("buttonsResponseMessage".equals(messageType)) {
            JsonNode buttonResponse = message.path("buttonsResponseMessage");
            if (!buttonResponse.isMissingNode()) {
                String id = buttonResponse.path("selectedButtonId").asText("");
                return id.isBlank() ? null : id;
            }
        }
        if ("templateButtonReplyMessage".equals(messageType)) {
            JsonNode templateReply = message.path("templateButtonReplyMessage");
            if (!templateReply.isMissingNode()) {
                String id = templateReply.path("selectedId").asText("");
                return id.isBlank() ? null : id;
            }
        }
        if ("interactiveMessage".equals(messageType)) {
            // Evolution API sends button presses as conversation text + contextInfo.stanzaId.
            // The stanzaId is the provider message ID of the button message we originally sent.
            String conversation = message.path("conversation").asText("");
            String stanzaId = message.path("contextInfo").path("stanzaId").asText("");
            if (!stanzaId.isBlank() && !conversation.isBlank()) {
                if (conversation.contains("Confirmar")) {
                    return "confirm:" + stanzaId;
                }
                if (conversation.contains("Cancelar")) {
                    return "cancel:" + stanzaId;
                }
            }
        }
        if ("listResponseMessage".equals(messageType)) {
            JsonNode listResponse = message.path("listResponseMessage");
            if (!listResponse.isMissingNode()) {
                String rowId = listResponse.path("singleSelectReply").path("selectedRowId").asText("");
                return rowId.isBlank() ? null : rowId;
            }
        }
        return null;
    }

    private JsonNode parseJson(String rawPayload) {
        if (rawPayload == null || rawPayload.isBlank()) {
            throw new InvalidWhatsappWebhookPayloadException(Map.of("body", "Payload must be valid JSON."));
        }

        try {
            return objectMapper.readTree(rawPayload);
        } catch (JsonProcessingException exception) {
            throw new InvalidWhatsappWebhookPayloadException(Map.of("body", "Payload must be valid JSON."));
        }
    }

    private JsonNode objectNode(JsonNode parent, String fieldName, String message, Map<String, String> fieldErrors) {
        JsonNode value = parent == null ? null : parent.get(fieldName);
        if (value == null || value.isNull() || !value.isObject()) {
            fieldErrors.put(path(fieldName, parent), message);
            return objectMapper.createObjectNode();
        }
        return value;
    }

    private String requiredText(
            JsonNode parent,
            String fieldName,
            String fieldPath,
            String message,
            Map<String, String> fieldErrors
    ) {
        JsonNode value = parent == null ? null : parent.get(fieldName);
        if (value == null || value.isNull() || !value.isTextual() || value.asText().isBlank()) {
            fieldErrors.put(fieldPath, message);
            return null;
        }
        return value.asText();
    }

    private Boolean requiredBoolean(
            JsonNode parent,
            String fieldName,
            String fieldPath,
            String message,
            Map<String, String> fieldErrors
    ) {
        JsonNode value = parent == null ? null : parent.get(fieldName);
        if (value == null || value.isNull() || !value.isBoolean()) {
            fieldErrors.put(fieldPath, message);
            return null;
        }
        return value.asBoolean();
    }

    private String path(String fieldName, JsonNode parent) {
        if (parent == null) {
            return fieldName;
        }
        return switch (fieldName) {
            case "data" -> "data";
            case "key" -> "data.key";
            default -> fieldName;
        };
    }
}
