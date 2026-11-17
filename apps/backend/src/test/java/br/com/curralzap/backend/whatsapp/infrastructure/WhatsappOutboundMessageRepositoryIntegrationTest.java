package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.support.BackendIntegrationTestSupport;
import br.com.curralzap.backend.users.domain.UserRole;
import br.com.curralzap.backend.whatsapp.application.WhatsappOutboundMessageRepository;
import br.com.curralzap.backend.whatsapp.domain.WhatsappOutboundMessage;
import br.com.curralzap.backend.whatsapp.domain.WhatsappOutboundStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WhatsappOutboundMessageRepositoryIntegrationTest extends BackendIntegrationTestSupport {

    @Autowired
    private WhatsappOutboundMessageRepository repository;

    @Test
    void savesQueuedOutboundMessagesAndAllowsCorrelationLookupsByInboundMessage() {
        UUID farmId = insertFarm("Fazenda Outbound");
        UUID userId = insertUser(farmId, "5511999990301", "Admin Outbound", UserRole.ADMIN, true).id();
        UUID inboundMessageId = insertWhatsappInboundMessage(
                "wamid.outbound.1",
                "5511999990301",
                farmId,
                userId,
                "corr-wa-out-1",
                "messages.upsert",
                "conversation",
                "RECEIVED",
                null
        );
        OffsetDateTime queuedAt = OffsetDateTime.parse("2026-04-05T13:00:00Z");

        WhatsappOutboundMessage queued = repository.save(WhatsappOutboundMessage.queue(
                inboundMessageId,
                farmId,
                userId,
                "corr-wa-out-1",
                "5511999990301",
                "text",
                "{\"text\":\"Confirmado\"}",
                queuedAt
        ));

        assertThat(queued.id()).isNotNull();
        assertThat(repository.findById(queued.id())).get()
                .extracting(
                        WhatsappOutboundMessage::status,
                        WhatsappOutboundMessage::correlationId,
                        WhatsappOutboundMessage::phoneNumber
                )
                .containsExactly(
                        WhatsappOutboundStatus.QUEUED,
                        "corr-wa-out-1",
                        "5511999990301"
                );

        List<WhatsappOutboundMessage> history = repository.findByInboundMessageIdOrderByCreatedAtAsc(inboundMessageId);
        assertThat(history).extracting(WhatsappOutboundMessage::id).containsExactly(queued.id());
    }

    @Test
    void updatesOutboundMessagesToSentOrFailedAndFindsSentMessageByProviderId() {
        UUID inboundMessageId = insertWhatsappInboundMessage(
                "wamid.outbound.2",
                "5511999990302",
                null,
                null,
                "corr-wa-out-2",
                "messages.upsert",
                "conversation",
                "RECEIVED",
                null
        );
        OffsetDateTime queuedAt = OffsetDateTime.parse("2026-04-05T14:00:00Z");

        WhatsappOutboundMessage queued = repository.save(WhatsappOutboundMessage.queue(
                inboundMessageId,
                null,
                null,
                "corr-wa-out-2",
                "5511999990302",
                "text",
                "{\"text\":\"Pendente\"}",
                queuedAt
        ));

        WhatsappOutboundMessage sent = repository.save(
                queued.markSent("wamid.provider.out.2", queuedAt.plusMinutes(1))
        );
        WhatsappOutboundMessage failed = repository.save(
                sent.markFailed("Evolution 502", queuedAt.plusMinutes(2))
        );

        assertThat(repository.findByProviderMessageId("wamid.provider.out.2")).get()
                .extracting(WhatsappOutboundMessage::status, WhatsappOutboundMessage::failureReason)
                .containsExactly(WhatsappOutboundStatus.FAILED, "Evolution 502");
        assertThat(failed.sentAt()).isEqualTo(queuedAt.plusMinutes(1));
    }
}
