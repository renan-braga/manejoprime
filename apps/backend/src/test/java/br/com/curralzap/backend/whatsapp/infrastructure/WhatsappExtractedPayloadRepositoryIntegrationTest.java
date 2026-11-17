package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.support.BackendIntegrationTestSupport;
import br.com.curralzap.backend.whatsapp.application.WhatsappExtractedPayloadRepository;
import br.com.curralzap.backend.whatsapp.domain.WhatsappExtractedPayload;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WhatsappExtractedPayloadRepositoryIntegrationTest extends BackendIntegrationTestSupport {

    @Autowired
    private WhatsappExtractedPayloadRepository repository;

    @Test
    void savesAndFindsStructuredPayloadByInboundMessageId() {
        var inboundMessageId = insertWhatsappInboundMessage(
                "wamid.extract.1",
                "5511999990401",
                null,
                null,
                "corr-wa-extract-1",
                "messages.upsert",
                "audioMessage",
                "TRANSCRIBED",
                null
        );
        OffsetDateTime extractedAt = OffsetDateTime.parse("2026-04-05T15:00:00Z");

        WhatsappExtractedPayload saved = repository.save(WhatsappExtractedPayload.extracted(
                inboundMessageId,
                "openai",
                "gpt-5.4-mini",
                "register_weighing",
                "{\"animalId\":\"A-1\",\"weightKg\":324.5}",
                extractedAt
        ));

        assertThat(saved.id()).isNotNull();
        assertThat(repository.findByInboundMessageId(inboundMessageId)).get()
                .extracting(
                        WhatsappExtractedPayload::provider,
                        WhatsappExtractedPayload::providerModel,
                        WhatsappExtractedPayload::intent,
                        WhatsappExtractedPayload::structuredPayload,
                        WhatsappExtractedPayload::extractedAt
                )
                .containsExactly(
                        "openai",
                        "gpt-5.4-mini",
                        "register_weighing",
                        "{\"animalId\":\"A-1\",\"weightKg\":324.5}",
                        extractedAt
                );
    }

    @Test
    void countsExtractedPayloadsByFarmIntentAndInboundReceivedDay() {
        UUID farmId = insertFarm("Fazenda Limite WhatsApp");
        var firstInboundMessageId = insertWhatsappInboundMessage(
                "wamid.extract.limit.1",
                "5511999990411",
                farmId,
                null,
                "corr-wa-extract-limit-1",
                "messages.upsert",
                "audioMessage",
                "INTENT_EXTRACTED",
                null
        );
        var secondInboundMessageId = insertWhatsappInboundMessage(
                "wamid.extract.limit.2",
                "5511999990412",
                farmId,
                null,
                "corr-wa-extract-limit-2",
                "messages.upsert",
                "audioMessage",
                "INTENT_EXTRACTED",
                null
        );
        var otherIntentInboundMessageId = insertWhatsappInboundMessage(
                "wamid.extract.limit.3",
                "5511999990413",
                farmId,
                null,
                "corr-wa-extract-limit-3",
                "messages.upsert",
                "audioMessage",
                "INTENT_EXTRACTED",
                null
        );
        OffsetDateTime extractedAt = OffsetDateTime.parse("2026-04-06T10:10:00Z");
        repository.save(WhatsappExtractedPayload.extracted(
                firstInboundMessageId,
                "openai",
                "gpt-5.4-mini",
                "register_weighing",
                "{\"intent\":\"register_weighing\"}",
                extractedAt
        ));
        repository.save(WhatsappExtractedPayload.extracted(
                secondInboundMessageId,
                "openai",
                "gpt-5.4-mini",
                "register_weighing",
                "{\"intent\":\"register_weighing\"}",
                extractedAt
        ));
        repository.save(WhatsappExtractedPayload.extracted(
                otherIntentInboundMessageId,
                "openai",
                "gpt-5.4-mini",
                "register_occurrence",
                "{\"intent\":\"register_occurrence\"}",
                extractedAt
        ));
        OffsetDateTime now = nowUtc();
        OffsetDateTime startOfDay = now.toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC);

        long count = repository.countByFarmIdAndIntentAndReceivedAtBetween(
                farmId,
                "register_weighing",
                startOfDay,
                startOfDay.plusDays(1)
        );

        assertThat(count).isEqualTo(2);
    }
}
