package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.whatsapp.application.WhatsappAudioTranscriber;
import br.com.curralzap.backend.whatsapp.application.WhatsappAudioTranscriptionRequest;
import br.com.curralzap.backend.whatsapp.application.WhatsappAudioTranscriptionResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
class OpenAiWhatsappAudioTranscriber implements WhatsappAudioTranscriber {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    OpenAiWhatsappAudioTranscriber(
            RestClient.Builder restClientBuilder,
            @Value("${curralzap.whatsapp.transcription.openai.base-url:https://api.openai.com}") String baseUrl,
            @Value("${curralzap.whatsapp.transcription.openai.api-key:}") String apiKey,
            @Value("${curralzap.whatsapp.transcription.openai.model:gpt-4o-mini-transcribe}") String model
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public WhatsappAudioTranscriptionResult transcribe(WhatsappAudioTranscriptionRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OpenAI API key is not configured for WhatsApp transcription.");
        }

        WhisperTranscriptionResponse response = restClient.post()
                .uri("/v1/audio/transcriptions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(buildRequestBody(request))
                .retrieve()
                .body(WhisperTranscriptionResponse.class);
        if (response == null || response.text() == null || response.text().isBlank()) {
            throw new IllegalStateException("Transcription provider returned an empty transcript.");
        }

        return new WhatsappAudioTranscriptionResult("openai", model, response.text());
    }

    private MultiValueMap<String, Object> buildRequestBody(WhatsappAudioTranscriptionRequest request) {
        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(MediaType.parseMediaType(defaultMimeType(request.mimeType())));

        ByteArrayResource resource = new ByteArrayResource(request.content()) {
            @Override
            public String getFilename() {
                return request.providerMessageId() + ".mp3";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new HttpEntity<>(resource, fileHeaders));
        body.add("model", model);
        body.add("language", "pt");
        body.add("prompt", "Boi 123 pesando 520 quilos. Lote 45 recebeu 200 quilos de ração. Animal A-7 vacina aftosa dose 2 lote L-01.");
        body.add("response_format", "json");
        return body;
    }

    private String defaultMimeType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return "audio/mpeg";
        }
        return mimeType;
    }

    private record WhisperTranscriptionResponse(String text) {
    }
}
