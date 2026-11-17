package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.support.BackendIntegrationTestSupport;
import br.com.curralzap.backend.whatsapp.application.WhatsappInboundMessageRepository;
import br.com.curralzap.backend.whatsapp.application.WhatsappTranscriptRepository;
import br.com.curralzap.backend.whatsapp.domain.WhatsappInboundMessage;
import br.com.curralzap.backend.whatsapp.domain.WhatsappTranscript;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class WhatsappTranscriptRepositoryIntegrationTest extends BackendIntegrationTestSupport {

    @Autowired
    private WhatsappTranscriptRepository repository;

    @Autowired
    private WhatsappInboundMessageRepository inboundMessageRepository;

    @Test
    void savesAndFindsTranscriptByInboundMessageId() {
        OffsetDateTime receivedAt = OffsetDateTime.parse("2026-04-04T12:00:00Z");
        OffsetDateTime transcribedAt = receivedAt.plusMinutes(3);
        WhatsappInboundMessage inboundMessage = inboundMessageRepository.save(WhatsappInboundMessage.receive(
                "wamid.transcript-repo-1",
                "messages.upsert",
                "5511999990001",
                null,
                null,
                "audioMessage",
                "{\"event\":\"messages.upsert\"}",
                receivedAt
        ));

        WhatsappTranscript saved = repository.save(WhatsappTranscript.transcribed(
                inboundMessage.id(),
                "openai",
                "gpt-4o-mini-transcribe",
                "Registrar peso de 18 arrobas.",
                transcribedAt
        ));

        assertThat(saved.id()).isNotNull();
        assertThat(repository.findByInboundMessageId(inboundMessage.id()))
                .get()
                .extracting(
                        WhatsappTranscript::provider,
                        WhatsappTranscript::providerModel,
                        WhatsappTranscript::transcript,
                        WhatsappTranscript::transcribedAt
                )
                .containsExactly(
                        "openai",
                        "gpt-4o-mini-transcribe",
                        "Registrar peso de 18 arrobas.",
                        transcribedAt
                );
    }
}
