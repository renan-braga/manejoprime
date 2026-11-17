package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.whatsapp.application.WhatsappOutboundDeliveryException;
import br.com.curralzap.backend.whatsapp.application.WhatsappTextOutboundMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
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
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpEvolutionWhatsappOutboundGatewayTest {

    @Test
    void sendsTextMessageThroughEvolutionSendTextEndpoint() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo("https://evolution.example/message/sendText/curralzap-test"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("apikey", "test-evolution-key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string(containsString("\"number\":\"5511999990001\"")))
                .andExpect(content().string(containsString("\"text\":\"⚖️ Boi 347: 520 kg — confirma?\"")))
                .andExpect(content().string(containsString("\"id\":\"wamid.in.1\"")))
                .andRespond(withSuccess("""
                        {
                          "key": {
                            "remoteJid": "5511999990001@s.whatsapp.net",
                            "fromMe": true,
                            "id": "wamid.out.1"
                          },
                          "status": "PENDING"
                        }
                        """, MediaType.APPLICATION_JSON));
        HttpEvolutionWhatsappOutboundGateway gateway = new HttpEvolutionWhatsappOutboundGateway(
                restClientBuilder,
                new ObjectMapper(),
                "https://evolution.example",
                "test-evolution-key",
                "curralzap-test",
                0
        );

        var result = gateway.sendText(new WhatsappTextOutboundMessage(
                "5511999990001",
                "⚖️ Boi 347: 520 kg — confirma?",
                "wamid.in.1"
        ));

        assertThat(result.providerMessageId()).isEqualTo("wamid.out.1");
        server.verify();
    }

    @Test
    void wrapsEvolutionErrorsAsDeliveryExceptions() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo("https://evolution.example/message/sendText/curralzap-test"))
                .andRespond(withServerError());
        HttpEvolutionWhatsappOutboundGateway gateway = new HttpEvolutionWhatsappOutboundGateway(
                restClientBuilder,
                new ObjectMapper(),
                "https://evolution.example",
                "test-evolution-key",
                "curralzap-test",
                0
        );

        assertThatThrownBy(() -> gateway.sendText(new WhatsappTextOutboundMessage(
                "5511999990001",
                "Mensagem",
                "wamid.in.2"
        )))
                .isInstanceOf(WhatsappOutboundDeliveryException.class)
                .hasMessageContaining("Evolution outbound delivery failed");
        server.verify();
    }

    @Test
    void failsFastWhenEvolutionApiKeyIsMissing() {
        HttpEvolutionWhatsappOutboundGateway gateway = new HttpEvolutionWhatsappOutboundGateway(
                RestClient.builder(),
                new ObjectMapper(),
                "https://evolution.example",
                "",
                "curralzap-test",
                0
        );

        assertThatThrownBy(() -> gateway.sendText(new WhatsappTextOutboundMessage(
                "5511999990001",
                "Mensagem",
                "wamid.in.3"
        )))
                .isInstanceOf(WhatsappOutboundDeliveryException.class)
                .hasMessage("Evolution API key is not configured for WhatsApp outbound delivery.");
    }
}
