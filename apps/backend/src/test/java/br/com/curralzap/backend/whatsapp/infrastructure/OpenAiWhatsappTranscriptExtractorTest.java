package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.whatsapp.application.WhatsappTranscriptExtractionRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenAiWhatsappTranscriptExtractorTest {

    @Test
    void sendsJsonExtractionRequestAndReturnsStructuredResult() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-api-key"))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, containsString(MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(content().string(containsString("\"model\":\"gpt-5.4-mini\"")))
                .andExpect(content().string(containsString("register_weighing")))
                .andExpect(content().string(containsString("Registrar peso do animal A-1")))
                .andRespond(withSuccess("""
                        {
                          "choices": [
                            {
                              "message": {
                                "content": "{\\"intent\\":\\"register_weighing\\",\\"fields\\":{\\"animalIdentifier\\":\\"A-1\\",\\"weightKg\\":324.5},\\"confidence\\":0.92}"
                              }
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));
        OpenAiWhatsappTranscriptExtractor extractor = new OpenAiWhatsappTranscriptExtractor(
                restClientBuilder,
                new ObjectMapper(),
                "https://api.openai.com",
                "test-api-key",
                "gpt-5.4-mini"
        );

        var result = extractor.extract(new WhatsappTranscriptExtractionRequest(
                "wamid.EXTRACT123",
                "Registrar peso do animal A-1 com 324,5 kg hoje."
        ));

        assertThat(result.provider()).isEqualTo("openai");
        assertThat(result.providerModel()).isEqualTo("gpt-5.4-mini");
        assertThat(result.intent()).isEqualTo("register_weighing");
        assertThat(result.fields()).containsEntry("animalIdentifier", "A-1");
        assertThat(result.fields()).containsEntry("weightKg", 324.5);
        assertThat(result.confidence()).isEqualTo(0.92);
        server.verify();
    }

    @Test
    void mapsUnknownIntentWithReason() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andRespond(withSuccess("""
                        {
                          "choices": [
                            {
                              "message": {
                                "content": "{\\"intent\\":\\"unknown\\",\\"fields\\":{},\\"confidence\\":0.82,\\"reason\\":\\"No supported workflow.\\"}"
                              }
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));
        OpenAiWhatsappTranscriptExtractor extractor = new OpenAiWhatsappTranscriptExtractor(
                restClientBuilder,
                new ObjectMapper(),
                "https://api.openai.com",
                "test-api-key",
                "gpt-5.4-mini"
        );

        var result = extractor.extract(new WhatsappTranscriptExtractionRequest(
                "wamid.EXTRACT456",
                "Bom dia."
        ));

        assertThat(result.intent()).isEqualTo("unknown");
        assertThat(result.reason()).isEqualTo("No supported workflow.");
        assertThat(result.confidence()).isEqualTo(0.82);
        server.verify();
    }

    @Test
    void failsFastWhenApiKeyIsMissing() {
        OpenAiWhatsappTranscriptExtractor extractor = new OpenAiWhatsappTranscriptExtractor(
                RestClient.builder(),
                new ObjectMapper(),
                "https://api.openai.com",
                "",
                "gpt-5.4-mini"
        );

        assertThatThrownBy(() -> extractor.extract(new WhatsappTranscriptExtractionRequest(
                "wamid.EXTRACT123",
                "Registrar peso."
        )))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("OpenAI API key is not configured for WhatsApp extraction.");
    }
}
