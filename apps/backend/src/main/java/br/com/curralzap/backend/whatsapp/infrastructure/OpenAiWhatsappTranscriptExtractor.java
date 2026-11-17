package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.whatsapp.application.WhatsappTranscriptExtractionRequest;
import br.com.curralzap.backend.whatsapp.application.WhatsappTranscriptExtractionResult;
import br.com.curralzap.backend.whatsapp.application.WhatsappTranscriptExtractor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
class OpenAiWhatsappTranscriptExtractor implements WhatsappTranscriptExtractor {

    private static final Logger log = LoggerFactory.getLogger(OpenAiWhatsappTranscriptExtractor.class);
    private static final TypeReference<Map<String, Object>> FIELD_MAP = new TypeReference<>() {
    };

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    OpenAiWhatsappTranscriptExtractor(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${curralzap.whatsapp.extraction.openai.base-url:https://api.openai.com}") String baseUrl,
            @Value("${curralzap.whatsapp.extraction.openai.api-key:}") String apiKey,
            @Value("${curralzap.whatsapp.extraction.openai.model:gpt-5.4-mini}") String model
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public WhatsappTranscriptExtractionResult extract(WhatsappTranscriptExtractionRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OpenAI API key is not configured for WhatsApp extraction.");
        }

        ChatCompletionResponse response = restClient.post()
                .uri("/v1/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "model", model,
                        "temperature", 0,
                        "response_format", Map.of("type", "json_object"),
                        "messages", List.of(
                                Map.of("role", "system", "content", systemPrompt()),
                                Map.of("role", "user", "content", request.transcript())
                        )
                ))
                .retrieve()
                .body(ChatCompletionResponse.class);

        String content = responseContent(response);
        log.info("Transcript extraction LLM raw response providerMessageId={} content={}", request.providerMessageId(), content);
        return resultFromJson(content);
    }

    private String systemPrompt() {
        return """
                Classify a Portuguese WhatsApp livestock field transcript and extract structured data.
                Return only a JSON object with: intent (string), fields (object), confidence (0.0–1.0),
                and optional reason (string). Leave missing or uncertain fields absent — never guess.

                Normalization rules:
                - Convert all numbers written in words to digits (e.g. "trezentos e quarenta e sete" → 347, "mil" → 1000).
                - If an animal identifier is spoken as individual digits separated by commas or spaces (e.g. "1, 2, 3" or "1 2 3"), concatenate them into the full number ("123"). Do NOT include "Boi", "Animal", or similar prefixes in animalIdentifier — extract only the tag itself.
                - Numeric field values (weightKg, quantityKg, withdrawalDays) must be numbers, not strings.

                Supported intents and their exact field names:

                register_weighing:
                  animalIdentifier (string) — required
                  weightKg (number) — required

                register_occurrence:
                  animalIdentifier (string) — required
                  description (string) — required
                  severity (string, optional: "low", "medium", "high")

                register_feeding:
                  animalIdentifier (string) OR lotName (string) — at least one required
                  quantityKg (number, optional)

                register_vaccine:
                  animalIdentifier (string) — required
                  vaccineName (string) — required
                  dose (string, optional)
                  batch (string, optional)

                register_medication:
                  animalIdentifier (string) — required
                  medicationName (string) — required
                  dosage (string, optional)
                  withdrawalDays (number, optional)

                help_menu:
                  (no fields) — use when the user asks for help, asks what they can do,
                  says "ajuda", "menu", "o que posso fazer", "como usar", etc.

                If the transcript does not match any supported workflow, use intent "unknown" and
                explain in the reason field.
                """;
    }

    private String responseContent(ChatCompletionResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()
                || response.choices().getFirst().message() == null
                || response.choices().getFirst().message().content() == null
                || response.choices().getFirst().message().content().isBlank()) {
            throw new IllegalStateException("Extraction provider returned an empty result.");
        }
        return response.choices().getFirst().message().content();
    }

    private WhatsappTranscriptExtractionResult resultFromJson(String content) {
        try {
            JsonNode root = objectMapper.readTree(stripJsonFence(content));
            String intent = requiredText(root, "intent");
            double confidence = requiredDouble(root, "confidence");
            if ("unknown".equals(intent)) {
                return WhatsappTranscriptExtractionResult.unknown(
                        "openai",
                        model,
                        optionalText(root, "reason"),
                        confidence
                );
            }
            JsonNode fieldsNode = root.path("fields");
            Map<String, Object> fields = fieldsNode.isMissingNode() || fieldsNode.isNull()
                    ? Map.of()
                    : objectMapper.convertValue(fieldsNode, FIELD_MAP);
            return WhatsappTranscriptExtractionResult.supported("openai", model, intent, fields, confidence);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw new IllegalStateException("Extraction provider returned malformed JSON.", exception);
        }
    }

    private String stripJsonFence(String content) {
        String trimmed = content.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        int firstNewline = trimmed.indexOf('\n');
        int lastFence = trimmed.lastIndexOf("```");
        if (firstNewline < 0 || lastFence <= firstNewline) {
            return trimmed;
        }
        return trimmed.substring(firstNewline + 1, lastFence).trim();
    }

    private String requiredText(JsonNode root, String fieldName) {
        String value = optionalText(root, fieldName);
        if (value.isBlank()) {
            throw new IllegalStateException("Extraction provider omitted " + fieldName + ".");
        }
        return value;
    }

    private String optionalText(JsonNode root, String fieldName) {
        JsonNode value = root.get(fieldName);
        if (value == null || value.isNull()) {
            return "";
        }
        return value.asText();
    }

    private double requiredDouble(JsonNode root, String fieldName) {
        JsonNode value = root.get(fieldName);
        if (value == null || !value.isNumber()) {
            throw new IllegalStateException("Extraction provider omitted " + fieldName + ".");
        }
        return value.asDouble();
    }

    private record ChatCompletionResponse(List<Choice> choices) {
    }

    private record Choice(Message message) {
    }

    private record Message(String content) {
    }
}
