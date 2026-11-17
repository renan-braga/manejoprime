package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.support.BackendIntegrationTestSupport;
import br.com.curralzap.backend.whatsapp.application.WhatsappInboundMessageRepository;
import br.com.curralzap.backend.whatsapp.domain.WhatsappInboundMessage;
import br.com.curralzap.backend.whatsapp.domain.WhatsappProcessingStatus;
import br.com.curralzap.backend.whatsapp.domain.WhatsappProcessingStep;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class WhatsappInboundMessageRepositoryIntegrationTest extends BackendIntegrationTestSupport {

    @Autowired
    private WhatsappInboundMessageRepository repository;

    @Test
    void savesInboundMessagesWithInitialProcessingStepAndProviderLookup() {
        OffsetDateTime receivedAt = OffsetDateTime.parse("2026-04-03T18:45:00Z");
        WhatsappInboundMessage saved = repository.save(WhatsappInboundMessage.receive(
                "wamid.AUDIT1",
                "messages.upsert",
                "5511999990001",
                null,
                null,
                "corr-wa-001",
                "conversation",
                "{\"event\":\"messages.upsert\"}",
                receivedAt
        ));

        assertThat(saved.id()).isNotNull();
        assertThat(saved.correlationId()).isEqualTo("corr-wa-001");
        assertThat(repository.findByProviderMessageId("wamid.missing")).isEmpty();
        assertThat(repository.findByProviderMessageId("wamid.AUDIT1"))
                .get()
                .extracting(
                        WhatsappInboundMessage::providerMessageId,
                        WhatsappInboundMessage::status,
                        WhatsappInboundMessage::receivedAt,
                        WhatsappInboundMessage::correlationId
                )
                .containsExactly("wamid.AUDIT1", WhatsappProcessingStatus.RECEIVED, receivedAt, "corr-wa-001");

        List<WhatsappProcessingStep> steps = repository.findProcessingSteps(saved.id());

        assertThat(steps)
                .extracting(
                        WhatsappProcessingStep::status,
                        WhatsappProcessingStep::details,
                        WhatsappProcessingStep::occurredAt
                )
                .containsExactly(tuple(
                        WhatsappProcessingStatus.RECEIVED,
                        null,
                        receivedAt
                ));
    }

    @Test
    void recordsProcessingStepHistoryAndRefreshesCurrentStatus() {
        OffsetDateTime receivedAt = OffsetDateTime.parse("2026-04-03T18:45:00Z");
        WhatsappInboundMessage saved = repository.save(WhatsappInboundMessage.receive(
                "wamid.AUDIT2",
                "messages.upsert",
                "5511999990002",
                null,
                null,
                "corr-wa-002",
                "audioMessage",
                "{\"event\":\"messages.upsert\"}",
                receivedAt
        ));
        OffsetDateTime transcribedAt = receivedAt.plusMinutes(7);

        WhatsappInboundMessage updated = repository.recordProcessingStep(
                saved.id(),
                WhatsappProcessingStatus.TRANSCRIBED,
                "Whisper transcription completed.",
                transcribedAt
        );

        assertThat(updated.status()).isEqualTo(WhatsappProcessingStatus.TRANSCRIBED);
        assertThat(repository.findByProviderMessageId("wamid.AUDIT2"))
                .get()
                .extracting(WhatsappInboundMessage::status)
                .isEqualTo(WhatsappProcessingStatus.TRANSCRIBED);

        assertThat(repository.findProcessingSteps(saved.id()))
                .extracting(
                        WhatsappProcessingStep::status,
                        WhatsappProcessingStep::details,
                        WhatsappProcessingStep::occurredAt
                )
                .containsExactly(
                        tuple(WhatsappProcessingStatus.RECEIVED, null, receivedAt),
                        tuple(WhatsappProcessingStatus.TRANSCRIBED, "Whisper transcription completed.", transcribedAt)
                );
    }

    @Test
    void storesFailureReasonAndSupportsFarmScopedInspectionLookups() {
        UUID farmId = insertFarm("Fazenda WhatsApp");
        OffsetDateTime firstReceivedAt = OffsetDateTime.parse("2026-04-03T18:00:00Z");
        OffsetDateTime secondReceivedAt = OffsetDateTime.parse("2026-04-03T19:00:00Z");

        WhatsappInboundMessage first = repository.save(WhatsappInboundMessage.receive(
                "wamid.audit-3",
                "messages.upsert",
                "5511999990101",
                farmId,
                null,
                "corr-wa-003",
                "audioMessage",
                "{\"event\":\"messages.upsert\",\"idx\":1}",
                firstReceivedAt
        ));
        WhatsappInboundMessage second = repository.save(WhatsappInboundMessage.receive(
                "wamid.audit-4",
                "messages.upsert",
                "5511999990102",
                farmId,
                null,
                "corr-wa-004",
                "conversation",
                "{\"event\":\"messages.upsert\",\"idx\":2}",
                secondReceivedAt
        ));

        WhatsappInboundMessage failed = repository.recordProcessingStep(
                first.id(),
                WhatsappProcessingStatus.FAILED,
                "Evolution publish timeout.",
                secondReceivedAt.plusMinutes(5)
        );

        assertThat(failed.failureReason()).isEqualTo("Evolution publish timeout.");
        assertThat(repository.findById(first.id())).get()
                .extracting(
                        WhatsappInboundMessage::status,
                        WhatsappInboundMessage::failureReason,
                        WhatsappInboundMessage::correlationId
                )
                .containsExactly(
                        WhatsappProcessingStatus.FAILED,
                        "Evolution publish timeout.",
                        "corr-wa-003"
                );

        assertThat(repository.findRecentByFarmId(farmId, 10))
                .extracting(WhatsappInboundMessage::id)
                .containsExactly(second.id(), first.id());
    }

    @Test
    void duplicateProviderMessageSaveReturnsExistingInboundRecordWithoutExtraReceiptStep() {
        OffsetDateTime receivedAt = OffsetDateTime.parse("2026-04-03T18:45:00Z");
        WhatsappInboundMessage first = repository.save(WhatsappInboundMessage.receive(
                "wamid.AUDIT-DUP",
                "messages.upsert",
                "5511999990007",
                null,
                null,
                "corr-wa-dup-1",
                "conversation",
                "{\"event\":\"messages.upsert\"}",
                receivedAt
        ));

        WhatsappInboundMessage duplicate = repository.save(WhatsappInboundMessage.receive(
                "wamid.AUDIT-DUP",
                "messages.upsert",
                "5511999990007",
                null,
                null,
                "corr-wa-dup-2",
                "conversation",
                "{\"event\":\"messages.upsert\"}",
                receivedAt.plusMinutes(1)
        ));

        assertThat(duplicate.id()).isEqualTo(first.id());
        assertThat(repository.findProcessingSteps(first.id()))
                .extracting(WhatsappProcessingStep::status, WhatsappProcessingStep::details)
                .containsExactly(tuple(WhatsappProcessingStatus.RECEIVED, null));
    }
}
