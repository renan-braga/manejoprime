package br.com.curralzap.backend.whatsapp.api;

import br.com.curralzap.backend.support.BackendApiIntegrationTestSupport;
import br.com.curralzap.backend.whatsapp.application.WhatsappMessagePublishFailedException;
import br.com.curralzap.backend.whatsapp.application.WhatsappMessageReceivedEvent;
import br.com.curralzap.backend.whatsapp.application.WhatsappReceivedMessagePublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WhatsappWebhookApiIntegrationTest extends BackendApiIntegrationTestSupport {

    private static final String SECRET = "curralzap-test-whatsapp-webhook-secret";

    @Autowired
    private CapturingWhatsappReceivedMessagePublisher publisher;

    @org.junit.jupiter.api.BeforeEach
    void resetCaptures() {
        publisher.events.clear();
        publisher.fail = false;
    }

    @Test
    void acceptsValidWebhookAndPersistsInboundMessage() throws Exception {
        UUID farmId = insertFarm("Fazenda WhatsApp");
        var user = insertUser(farmId, "5511999990001", "Admin User", "admin", true);

        mockMvc.perform(post("/api/v1/whatsapp/webhooks/evolution")
                        .header("X-Request-Id", "req-wa-003-api-ok")
                        .header("X-Webhook-Secret", SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "event": "messages.upsert",
                                  "data": {
                                    "key": {
                                      "id": "wamid.ABC123",
                                      "remoteJid": "5511999990001@s.whatsapp.net",
                                      "fromMe": false
                                    },
                                    "messageType": "conversation",
                                    "message": {
                                      "conversation": "Peso 18 arrobas"
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.status").value("accepted"));

        Integer persistedCount = jdbcTemplate.queryForObject(
                "select count(*) from whatsapp_inbound_messages where provider_message_id = ?",
                Integer.class,
                "wamid.ABC123"
        );
        assertThat(persistedCount).isEqualTo(1);

        var persisted = jdbcTemplate.queryForMap(
                """
                        select phone_number, event_type, message_type, status, farm_id, user_id
                        from whatsapp_inbound_messages
                        where provider_message_id = ?
                        """,
                "wamid.ABC123"
        );
        assertThat(persisted.get("phone_number")).isEqualTo("5511999990001");
        assertThat(persisted.get("event_type")).isEqualTo("messages.upsert");
        assertThat(persisted.get("message_type")).isEqualTo("conversation");
        assertThat(persisted.get("status")).isEqualTo("RECEIVED");
        assertThat(persisted.get("farm_id").toString()).isEqualTo(user.farmId().toString());
        assertThat(persisted.get("user_id").toString()).isEqualTo(user.id().toString());
        assertThat(publisher.events).singleElement()
                .satisfies(event -> {
                    assertThat(event.correlationId()).isEqualTo("req-wa-003-api-ok");
                    assertThat(event.providerMessageId()).isEqualTo("wamid.ABC123");
                    assertThat(event.inboundMessageId()).isNotNull();
                });
    }

    @Test
    void rejectsWebhookWhenSecretIsInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/whatsapp/webhooks/evolution")
                        .header("X-Webhook-Secret", "wrong-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "event": "messages.upsert",
                                  "data": {
                                    "key": {
                                      "id": "wamid.BADSECRET",
                                      "remoteJid": "5511999990001@s.whatsapp.net",
                                      "fromMe": false
                                    },
                                    "messageType": "conversation"
                                  }
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value("authentication_failed"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());

        Integer persistedCount = jdbcTemplate.queryForObject("select count(*) from whatsapp_inbound_messages", Integer.class);
        assertThat(persistedCount).isZero();
        assertThat(publisher.events).isEmpty();
    }

    @Test
    void returnsValidationEnvelopeForMalformedPayload() throws Exception {
        mockMvc.perform(post("/api/v1/whatsapp/webhooks/evolution")
                        .header("X-Webhook-Secret", SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "event": "messages.upsert",
                                  "data": {
                                    "key": {
                                      "remoteJid": "5511999990001@s.whatsapp.net",
                                      "fromMe": false
                                    },
                                    "messageType": "conversation"
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value("validation_failed"))
                .andExpect(jsonPath("$.fieldErrors['data.key.id']").value("Provider message ID is required."))
                .andExpect(jsonPath("$.requestId").isNotEmpty());

        assertThat(publisher.events).isEmpty();
    }

    @Test
    void returnsExplicitFailureWhenPublishingReceivedEventFails() throws Exception {
        UUID farmId = insertFarm("Fazenda WhatsApp");
        insertUser(farmId, "5511999990001", "Admin User", "admin", true);
        publisher.fail = true;

        mockMvc.perform(post("/api/v1/whatsapp/webhooks/evolution")
                        .header("X-Request-Id", "req-wa-003-api-fail")
                        .header("X-Webhook-Secret", SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "event": "messages.upsert",
                                  "data": {
                                    "key": {
                                      "id": "wamid.FAIL123",
                                      "remoteJid": "5511999990001@s.whatsapp.net",
                                      "fromMe": false
                                    },
                                    "messageType": "conversation"
                                  }
                                }
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value("dependency_failed"))
                .andExpect(jsonPath("$.requestId").value("req-wa-003-api-fail"));

        Integer persistedCount = jdbcTemplate.queryForObject(
                "select count(*) from whatsapp_inbound_messages where provider_message_id = ?",
                Integer.class,
                "wamid.FAIL123"
        );
        assertThat(persistedCount).isEqualTo(1);
    }

    @Test
    void acceptsDuplicateWebhookWithoutPersistingOrPublishingTwice() throws Exception {
        UUID farmId = insertFarm("Fazenda Duplicada");
        insertUser(farmId, "5511999990008", "Admin User", "admin", true);

        mockMvc.perform(post("/api/v1/whatsapp/webhooks/evolution")
                        .header("X-Request-Id", "req-wa-010-dup-1")
                        .header("X-Webhook-Secret", SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "event": "messages.upsert",
                                  "data": {
                                    "key": {
                                      "id": "wamid.DUP123",
                                      "remoteJid": "5511999990008@s.whatsapp.net",
                                      "fromMe": false
                                    },
                                    "messageType": "conversation"
                                  }
                                }
                                """))
                .andExpect(status().isAccepted());

        mockMvc.perform(post("/api/v1/whatsapp/webhooks/evolution")
                        .header("X-Request-Id", "req-wa-010-dup-2")
                        .header("X-Webhook-Secret", SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "event": "messages.upsert",
                                  "data": {
                                    "key": {
                                      "id": "wamid.DUP123",
                                      "remoteJid": "5511999990008@s.whatsapp.net",
                                      "fromMe": false
                                    },
                                    "messageType": "conversation"
                                  }
                                }
                                """))
                .andExpect(status().isAccepted());

        Integer persistedCount = jdbcTemplate.queryForObject(
                "select count(*) from whatsapp_inbound_messages where provider_message_id = ?",
                Integer.class,
                "wamid.DUP123"
        );
        assertThat(persistedCount).isEqualTo(1);
        assertThat(publisher.events).singleElement()
                .extracting(WhatsappMessageReceivedEvent::correlationId)
                .isEqualTo("req-wa-010-dup-1");
    }

    @Test
    void duplicateWebhookRetriesReceivedEventAfterInitialPublishFailure() throws Exception {
        UUID farmId = insertFarm("Fazenda Retry");
        insertUser(farmId, "5511999990009", "Admin User", "admin", true);
        publisher.fail = true;

        mockMvc.perform(post("/api/v1/whatsapp/webhooks/evolution")
                        .header("X-Request-Id", "req-wa-010-fail-1")
                        .header("X-Webhook-Secret", SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "event": "messages.upsert",
                                  "data": {
                                    "key": {
                                      "id": "wamid.RETRY123",
                                      "remoteJid": "5511999990009@s.whatsapp.net",
                                      "fromMe": false
                                    },
                                    "messageType": "conversation"
                                  }
                                }
                                """))
                .andExpect(status().isServiceUnavailable());

        publisher.fail = false;

        mockMvc.perform(post("/api/v1/whatsapp/webhooks/evolution")
                        .header("X-Request-Id", "req-wa-010-fail-2")
                        .header("X-Webhook-Secret", SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "event": "messages.upsert",
                                  "data": {
                                    "key": {
                                      "id": "wamid.RETRY123",
                                      "remoteJid": "5511999990009@s.whatsapp.net",
                                      "fromMe": false
                                    },
                                    "messageType": "conversation"
                                  }
                                }
                                """))
                .andExpect(status().isAccepted());

        Integer persistedCount = jdbcTemplate.queryForObject(
                "select count(*) from whatsapp_inbound_messages where provider_message_id = ?",
                Integer.class,
                "wamid.RETRY123"
        );
        assertThat(persistedCount).isEqualTo(1);
        assertThat(publisher.events).singleElement()
                .extracting(WhatsappMessageReceivedEvent::correlationId)
                .isEqualTo("req-wa-010-fail-2");
        assertThat(jdbcTemplate.queryForObject(
                "select status from whatsapp_inbound_messages where provider_message_id = ?",
                String.class,
                "wamid.RETRY123"
        )).isEqualTo("RECEIVED");
    }

    @TestConfiguration
    static class WhatsappWebhookApiTestConfiguration {

        @Bean
        @Primary
        CapturingWhatsappReceivedMessagePublisher capturingWhatsappReceivedMessagePublisher() {
            return new CapturingWhatsappReceivedMessagePublisher();
        }
    }

    static class CapturingWhatsappReceivedMessagePublisher implements WhatsappReceivedMessagePublisher {

        private final List<WhatsappMessageReceivedEvent> events = new ArrayList<>();
        private boolean fail;

        @Override
        public void publishReceived(WhatsappMessageReceivedEvent event) {
            if (fail) {
                throw new WhatsappMessagePublishFailedException(
                        event.inboundMessageId(),
                        event.providerMessageId(),
                        new IllegalStateException("Kafka unavailable")
                );
            }
            events.add(event);
        }
    }
}
