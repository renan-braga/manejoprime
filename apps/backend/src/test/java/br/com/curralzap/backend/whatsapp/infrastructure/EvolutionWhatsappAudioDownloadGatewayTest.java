package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.whatsapp.application.DownloadedWhatsappAudio;
import br.com.curralzap.backend.whatsapp.application.WhatsappAudioPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class EvolutionWhatsappAudioDownloadGatewayTest {

    private static final String BASE_URL = "https://evolution.example";
    private static final String API_KEY = "test-api-key";
    private static final String INSTANCE = "curralzap-test";

    private EvolutionWhatsappAudioDownloadGateway buildGateway(RestClient.Builder restClientBuilder) {
        return new EvolutionWhatsappAudioDownloadGateway(
                restClientBuilder,
                new RestTemplateBuilder(),
                new ObjectMapper(),
                BASE_URL,
                API_KEY,
                INSTANCE
        );
    }

    @Test
    void downloadsAudioViaEvolutionApiWhenRawDataJsonIsPresent() {
        byte[] audioBytes = "ogg-audio-content".getBytes();
        String base64Content = Base64.getEncoder().encodeToString(audioBytes);
        String rawDataJson = """
                {"key":{"id":"wamid.test","remoteJid":"5511900000000@s.whatsapp.net","fromMe":false},\
                "messageType":"audioMessage",\
                "message":{"audioMessage":{"url":"https://lookaside.fbsbx.com/attachments/","mimetype":"audio/ogg; codecs=opus"}}}""";

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE_URL + "/chat/getBase64FromMediaMessage/" + INSTANCE))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("apikey", API_KEY))
                .andRespond(withSuccess(
                        """
                        {"base64":"%s","mimetype":"audio/ogg; codecs=opus","fileName":"audio.ogg"}
                        """.formatted(base64Content),
                        MediaType.APPLICATION_JSON
                ));

        WhatsappAudioPayload payload = new WhatsappAudioPayload(
                "https://lookaside.fbsbx.com/attachments/",
                "audio/ogg; codecs=opus",
                null,
                rawDataJson
        );

        DownloadedWhatsappAudio result = buildGateway(builder).download(payload);

        assertThat(result.content()).isEqualTo(audioBytes);
        assertThat(result.contentType()).isEqualTo("audio/ogg; codecs=opus");
        server.verify();
    }

    @Test
    void stripsDataUriPrefixFromBase64Response() {
        byte[] audioBytes = "ptt-audio-bytes".getBytes();
        String base64Content = Base64.getEncoder().encodeToString(audioBytes);
        String rawDataJson = """
                {"key":{"id":"wamid.ptt"},"messageType":"audioMessage","message":{"audioMessage":{}}}""";

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE_URL + "/chat/getBase64FromMediaMessage/" + INSTANCE))
                .andRespond(withSuccess(
                        """
                        {"base64":"data:audio/ogg;codecs=opus;base64,%s","mimetype":"audio/ogg"}
                        """.formatted(base64Content),
                        MediaType.APPLICATION_JSON
                ));

        WhatsappAudioPayload payload = new WhatsappAudioPayload(
                "https://lookaside.fbsbx.com/ptt/",
                "audio/ogg",
                null,
                rawDataJson
        );

        DownloadedWhatsappAudio result = buildGateway(builder).download(payload);

        assertThat(result.content()).isEqualTo(audioBytes);
    }

    @Test
    void usesFallbackMimeTypeWhenEvolutionApiResponseHasNone() {
        byte[] audioBytes = "audio-no-mime".getBytes();
        String base64Content = Base64.getEncoder().encodeToString(audioBytes);
        String rawDataJson = """
                {"key":{"id":"wamid.nomime"},"messageType":"audioMessage","message":{"audioMessage":{}}}""";

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE_URL + "/chat/getBase64FromMediaMessage/" + INSTANCE))
                .andRespond(withSuccess(
                        """
                        {"base64":"%s"}
                        """.formatted(base64Content),
                        MediaType.APPLICATION_JSON
                ));

        WhatsappAudioPayload payload = new WhatsappAudioPayload(
                "https://lookaside.fbsbx.com/audio/",
                "audio/ogg; codecs=opus",
                null,
                rawDataJson
        );

        DownloadedWhatsappAudio result = buildGateway(builder).download(payload);

        assertThat(result.content()).isEqualTo(audioBytes);
        assertThat(result.contentType()).isEqualTo("audio/ogg; codecs=opus");
    }

    @Test
    void throwsWhenEvolutionApiReturnsEmptyBase64() {
        String rawDataJson = """
                {"key":{"id":"wamid.empty"},"messageType":"audioMessage","message":{"audioMessage":{}}}""";

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer.bindTo(builder).build()
                .expect(requestTo(BASE_URL + "/chat/getBase64FromMediaMessage/" + INSTANCE))
                .andRespond(withSuccess(
                        """
                        {"base64":"","mimetype":"audio/ogg"}
                        """,
                        MediaType.APPLICATION_JSON
                ));

        WhatsappAudioPayload payload = new WhatsappAudioPayload(
                "https://lookaside.fbsbx.com/empty/",
                "audio/ogg",
                null,
                rawDataJson
        );

        assertThatThrownBy(() -> buildGateway(builder).download(payload))
                .isInstanceOf(org.springframework.web.client.RestClientException.class)
                .hasMessageContaining("no base64 content");
    }
}
