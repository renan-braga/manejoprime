package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.whatsapp.application.DownloadedWhatsappAudio;
import br.com.curralzap.backend.whatsapp.application.WhatsappAudioDownloadGateway;
import br.com.curralzap.backend.whatsapp.application.WhatsappAudioPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
class EvolutionWhatsappAudioDownloadGateway implements WhatsappAudioDownloadGateway {

    private static final Logger log = LoggerFactory.getLogger(EvolutionWhatsappAudioDownloadGateway.class);

    private final RestClient evolutionClient;
    private final RestTemplate httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String instance;

    EvolutionWhatsappAudioDownloadGateway(
            RestClient.Builder restClientBuilder,
            RestTemplateBuilder restTemplateBuilder,
            ObjectMapper objectMapper,
            @Value("${curralzap.whatsapp.evolution.base-url:http://localhost:8080}") String baseUrl,
            @Value("${curralzap.whatsapp.evolution.api-key:}") String apiKey,
            @Value("${curralzap.whatsapp.evolution.instance:curralzap}") String instance
    ) {
        this.evolutionClient = restClientBuilder.baseUrl(baseUrl).build();
        this.httpClient = restTemplateBuilder.build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.instance = instance;
    }

    @Override
    public DownloadedWhatsappAudio download(WhatsappAudioPayload payload) {
        if (payload.rawDataJson() != null) {
            return downloadViaEvolutionApi(payload);
        }
        return downloadDirectly(payload);
    }

    private DownloadedWhatsappAudio downloadViaEvolutionApi(WhatsappAudioPayload payload) {
        try {
            JsonNode messageData = objectMapper.readTree(payload.rawDataJson());
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("message", messageData);
            body.put("convertToMp4", false);

            String responseBody = evolutionClient.post()
                    .uri("/chat/getBase64FromMediaMessage/{instance}", instance)
                    .header("apikey", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return parseBase64Response(responseBody, payload.mimeType());
        } catch (JsonProcessingException exception) {
            throw new RestClientException("Failed to parse Evolution API media download response.", exception);
        }
    }

    private DownloadedWhatsappAudio parseBase64Response(String responseBody, String fallbackMimeType) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(responseBody);
        String base64 = root.path("base64").asText("");
        if (base64.isBlank()) {
            throw new RestClientException("Evolution API media download returned no base64 content.");
        }
        // Strip data URI prefix if present (e.g. "data:audio/ogg;base64,")
        int commaIndex = base64.indexOf(',');
        if (commaIndex >= 0) {
            base64 = base64.substring(commaIndex + 1);
        }
        byte[] content = Base64.getDecoder().decode(base64);
        String mimeType = root.path("mimetype").asText("");
        if (mimeType.isBlank()) {
            mimeType = fallbackMimeType;
        }
        return new DownloadedWhatsappAudio(mimeType, content);
    }

    private DownloadedWhatsappAudio downloadDirectly(WhatsappAudioPayload payload) {
        log.debug("Downloading audio directly from URL (no Evolution API data available)");
        ResponseEntity<byte[]> response = httpClient.getForEntity(payload.mediaUrl(), byte[].class);
        byte[] body = response.getBody();
        if (body == null || body.length == 0) {
            throw new RestClientException("Audio media download returned an empty body.");
        }
        String contentType = response.getHeaders().getContentType() != null
                ? response.getHeaders().getContentType().toString()
                : payload.mimeType();
        return new DownloadedWhatsappAudio(contentType, body);
    }
}
