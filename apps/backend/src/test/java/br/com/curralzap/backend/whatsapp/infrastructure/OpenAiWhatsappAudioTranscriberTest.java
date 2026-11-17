package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.whatsapp.application.WhatsappAudioTranscriptionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenAiWhatsappAudioTranscriberTest {

    @Test
    void sendsMultipartTranscriptionRequestAndReturnsTranscript() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo("https://api.openai.com/v1/audio/transcriptions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-api-key"))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, containsString(MediaType.MULTIPART_FORM_DATA_VALUE)))
                .andExpect(content().string(containsString("name=\"model\"")))
                .andExpect(content().string(containsString("gpt-4o-mini-transcribe")))
                .andExpect(content().string(containsString("name=\"response_format\"")))
                .andExpect(content().string(containsString("name=\"file\"; filename=\"wamid.TRANSCRIBE123.mp3\"")))
                .andRespond(withSuccess("""
                        {"text":"Bom dia, curral."}
                        """, MediaType.APPLICATION_JSON));
        OpenAiWhatsappAudioTranscriber transcriber = new OpenAiWhatsappAudioTranscriber(
                restClientBuilder,
                "https://api.openai.com",
                "test-api-key",
                "gpt-4o-mini-transcribe"
        );

        var result = transcriber.transcribe(new WhatsappAudioTranscriptionRequest(
                "wamid.TRANSCRIBE123",
                "audio/mpeg",
                "audio-data".getBytes(StandardCharsets.UTF_8)
        ));

        assertThat(result.provider()).isEqualTo("openai");
        assertThat(result.providerModel()).isEqualTo("gpt-4o-mini-transcribe");
        assertThat(result.transcript()).isEqualTo("Bom dia, curral.");
        server.verify();
    }

    @Test
    void failsFastWhenApiKeyIsMissing() {
        OpenAiWhatsappAudioTranscriber transcriber = new OpenAiWhatsappAudioTranscriber(
                RestClient.builder(),
                "https://api.openai.com",
                "",
                "gpt-4o-mini-transcribe"
        );

        assertThatThrownBy(() -> transcriber.transcribe(new WhatsappAudioTranscriptionRequest(
                "wamid.TRANSCRIBE123",
                "audio/mpeg",
                "audio-data".getBytes(StandardCharsets.UTF_8)
        )))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("OpenAI API key is not configured for WhatsApp transcription.");
    }
}
