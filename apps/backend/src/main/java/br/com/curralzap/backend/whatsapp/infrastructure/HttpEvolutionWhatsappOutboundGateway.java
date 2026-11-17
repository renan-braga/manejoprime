package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.whatsapp.application.WhatsappButtonsOutboundMessage;
import br.com.curralzap.backend.whatsapp.application.WhatsappOutboundDeliveryException;
import br.com.curralzap.backend.whatsapp.application.WhatsappOutboundDeliveryResult;
import br.com.curralzap.backend.whatsapp.application.WhatsappOutboundGateway;
import br.com.curralzap.backend.whatsapp.application.WhatsappTextOutboundMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
class HttpEvolutionWhatsappOutboundGateway implements WhatsappOutboundGateway {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpEvolutionWhatsappOutboundGateway.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String instance;
    private final int messageDelayMs;

    HttpEvolutionWhatsappOutboundGateway(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${curralzap.whatsapp.evolution.base-url:http://localhost:8080}") String baseUrl,
            @Value("${curralzap.whatsapp.evolution.api-key:}") String apiKey,
            @Value("${curralzap.whatsapp.evolution.instance:curralzap}") String instance,
            @Value("${curralzap.whatsapp.evolution.message-delay-ms:0}") int messageDelayMs
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.instance = instance;
        this.messageDelayMs = messageDelayMs;
    }

    @Override
    public WhatsappOutboundDeliveryResult sendText(WhatsappTextOutboundMessage message) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new WhatsappOutboundDeliveryException("Evolution API key is not configured for WhatsApp outbound delivery.");
        }
        if (instance == null || instance.isBlank()) {
            throw new WhatsappOutboundDeliveryException("Evolution instance is not configured for WhatsApp outbound delivery.");
        }

        try {
            LOGGER.info(
                    "Sending WhatsApp outbound text phoneNumber={} quotedProviderMessageId={}",
                    message.phoneNumber(),
                    message.quotedProviderMessageId()
            );
            String responseBody = restClient.post()
                    .uri("/message/sendText/{instance}", instance)
                    .header("apikey", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody(message))
                    .retrieve()
                    .body(String.class);
            WhatsappOutboundDeliveryResult result = new WhatsappOutboundDeliveryResult(providerMessageId(responseBody));
            LOGGER.info("Sent WhatsApp outbound text providerMessageId={}", result.providerMessageId());
            return result;
        } catch (RestClientException exception) {
            LOGGER.warn("Evolution outbound delivery failed phoneNumber={}", message.phoneNumber(), exception);
            throw new WhatsappOutboundDeliveryException("Evolution outbound delivery failed: " + exception.getMessage(), exception);
        }
    }

    @Override
    public WhatsappOutboundDeliveryResult sendButtons(WhatsappButtonsOutboundMessage message) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new WhatsappOutboundDeliveryException("Evolution API key is not configured for WhatsApp outbound delivery.");
        }
        if (instance == null || instance.isBlank()) {
            throw new WhatsappOutboundDeliveryException("Evolution instance is not configured for WhatsApp outbound delivery.");
        }
        try {
            LOGGER.info(
                    "Sending WhatsApp outbound buttons phoneNumber={} quotedProviderMessageId={}",
                    message.phoneNumber(),
                    message.quotedProviderMessageId()
            );
            String responseBody = restClient.post()
                    .uri("/message/sendButtons/{instance}", instance)
                    .header("apikey", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buttonsRequestBody(message))
                    .retrieve()
                    .body(String.class);
            WhatsappOutboundDeliveryResult result = new WhatsappOutboundDeliveryResult(providerMessageId(responseBody));
            LOGGER.info("Sent WhatsApp outbound buttons providerMessageId={}", result.providerMessageId());
            return result;
        } catch (RestClientException exception) {
            LOGGER.warn("Evolution buttons delivery failed phoneNumber={}", message.phoneNumber(), exception);
            throw new WhatsappOutboundDeliveryException("Evolution buttons delivery failed: " + exception.getMessage(), exception);
        }
    }

    private Map<String, Object> requestBody(WhatsappTextOutboundMessage message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("number", message.phoneNumber());
        body.put("text", message.text());
        body.put("delay", messageDelayMs);
        body.put("linkPreview", false);
        if (message.quotedProviderMessageId() != null && !message.quotedProviderMessageId().isBlank()) {
            body.put("quoted", Map.of(
                    "key", Map.of("id", message.quotedProviderMessageId()),
                    "message", Map.of("conversation", "")
            ));
        }
        return body;
    }

    private Map<String, Object> buttonsRequestBody(WhatsappButtonsOutboundMessage message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("number", message.phoneNumber());
        body.put("title", message.title());
        body.put("description", message.description());
        body.put("footer", message.footer());
        body.put("delay", messageDelayMs);
        List<Map<String, String>> buttons = message.buttons().stream()
                .map(b -> Map.of("type", "reply", "displayText", b.displayText(), "id", b.id()))
                .toList();
        body.put("buttons", buttons);
        if (message.quotedProviderMessageId() != null && !message.quotedProviderMessageId().isBlank()) {
            body.put("quoted", Map.of(
                    "key", Map.of("id", message.quotedProviderMessageId()),
                    "message", Map.of("conversation", "")
            ));
        }
        return body;
    }

    private String providerMessageId(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String providerMessageId = root.path("key").path("id").asText("");
            return providerMessageId.isBlank() ? null : providerMessageId;
        } catch (JsonProcessingException exception) {
            throw new WhatsappOutboundDeliveryException("Evolution outbound delivery returned malformed JSON.", exception);
        }
    }
}
