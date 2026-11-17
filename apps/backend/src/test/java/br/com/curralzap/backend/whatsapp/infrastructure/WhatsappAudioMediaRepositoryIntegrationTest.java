package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.support.BackendIntegrationTestSupport;
import br.com.curralzap.backend.whatsapp.application.WhatsappAudioMediaRepository;
import br.com.curralzap.backend.whatsapp.domain.WhatsappAudioMedia;
import br.com.curralzap.backend.whatsapp.domain.WhatsappInboundMessage;
import br.com.curralzap.backend.shared.application.MediaReferenceRepository;
import br.com.curralzap.backend.shared.domain.MediaReferenceOwner;
import br.com.curralzap.backend.shared.domain.MediaReferenceOwnerType;
import br.com.curralzap.backend.shared.domain.MediaReferencePurpose;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class WhatsappAudioMediaRepositoryIntegrationTest extends BackendIntegrationTestSupport {

    @Autowired
    private WhatsappAudioMediaRepository repository;

    @Autowired
    private br.com.curralzap.backend.whatsapp.application.WhatsappInboundMessageRepository inboundMessageRepository;

    @Autowired
    private MediaReferenceRepository mediaReferenceRepository;

    @Test
    void savesAndFindsDownloadedAudioMetadataByInboundMessageId() {
        OffsetDateTime receivedAt = OffsetDateTime.parse("2026-04-04T11:00:00Z");
        WhatsappInboundMessage inboundMessage = inboundMessageRepository.save(WhatsappInboundMessage.receive(
                "wamid.audio-repo-1",
                "messages.upsert",
                "5511999990001",
                null,
                null,
                "audioMessage",
                "{\"event\":\"messages.upsert\"}",
                receivedAt
        ));

        WhatsappAudioMedia saved = repository.save(WhatsappAudioMedia.downloaded(
                inboundMessage.id(),
                "https://media.example/audio-repo-1.ogg",
                "audio/ogg",
                "whatsapp/audio/audio-repo-1.ogg",
                receivedAt.plusMinutes(2)
        ));

        assertThat(saved.id()).isNotNull();
        assertThat(repository.findByInboundMessageId(inboundMessage.id()))
                .get()
                .extracting(
                        WhatsappAudioMedia::inboundMessageId,
                        WhatsappAudioMedia::sourceUrl,
                        WhatsappAudioMedia::mimeType,
                        WhatsappAudioMedia::storagePath
                )
                .containsExactly(
                        inboundMessage.id(),
                        "https://media.example/audio-repo-1.ogg",
                        "audio/ogg",
                        "whatsapp/audio/audio-repo-1.ogg"
                );
    }

    @Test
    void savesAndFindsTranscodedAudioMetadataByInboundMessageId() {
        OffsetDateTime receivedAt = OffsetDateTime.parse("2026-04-04T12:00:00Z");
        OffsetDateTime downloadedAt = receivedAt.plusMinutes(2);
        OffsetDateTime transcodedAt = downloadedAt.plusMinutes(1);
        WhatsappInboundMessage inboundMessage = inboundMessageRepository.save(WhatsappInboundMessage.receive(
                "wamid.audio-repo-2",
                "messages.upsert",
                "5511999990002",
                null,
                null,
                "audioMessage",
                "{\"event\":\"messages.upsert\"}",
                receivedAt
        ));

        WhatsappAudioMedia saved = repository.save(WhatsappAudioMedia.downloaded(
                inboundMessage.id(),
                "https://media.example/audio-repo-2.ogg",
                "audio/ogg",
                "whatsapp/audio/audio-repo-2.ogg",
                downloadedAt
        ).markTranscoded(
                "audio/mpeg",
                "whatsapp/audio/audio-repo-2-transcoded.mp3",
                transcodedAt
        ));

        assertThat(saved.id()).isNotNull();
        assertThat(repository.findByInboundMessageId(inboundMessage.id()))
                .get()
                .extracting(
                        WhatsappAudioMedia::transcodedMimeType,
                        WhatsappAudioMedia::transcodedStoragePath,
                        WhatsappAudioMedia::transcodedAt
                )
                .containsExactly(
                        "audio/mpeg",
                        "whatsapp/audio/audio-repo-2-transcoded.mp3",
                        transcodedAt
                );
    }

    @Test
    void savesSharedMediaReferencesForDownloadedAndTranscodedAudioMetadata() {
        OffsetDateTime receivedAt = OffsetDateTime.parse("2026-04-28T13:00:00Z");
        OffsetDateTime downloadedAt = receivedAt.plusMinutes(2);
        OffsetDateTime transcodedAt = downloadedAt.plusMinutes(1);
        WhatsappInboundMessage inboundMessage = inboundMessageRepository.save(WhatsappInboundMessage.receive(
                "wamid.audio-repo-media-reference",
                "messages.upsert",
                "5511999990003",
                null,
                null,
                "audioMessage",
                "{\"event\":\"messages.upsert\"}",
                receivedAt
        ));

        WhatsappAudioMedia saved = repository.save(WhatsappAudioMedia.downloaded(
                inboundMessage.id(),
                "https://media.example/audio-repo-media-reference.ogg",
                "audio/ogg",
                "whatsapp/audio/audio-repo-media-reference.ogg",
                downloadedAt
        ).markTranscoded(
                "audio/mpeg",
                "whatsapp/audio/audio-repo-media-reference.mp3",
                transcodedAt
        ));

        MediaReferenceOwner owner = new MediaReferenceOwner(MediaReferenceOwnerType.WHATSAPP_AUDIO_MEDIA, saved.id());
        assertThat(mediaReferenceRepository.findByOwner(owner))
                .extracting(reference -> reference.purpose().databaseValue())
                .containsExactlyInAnyOrder(
                        "whatsapp_source_audio",
                        "whatsapp_downloaded_audio",
                        "whatsapp_transcoded_audio"
                );
        assertThat(mediaReferenceRepository.findByOwnerAndPurpose(owner, MediaReferencePurpose.WHATSAPP_SOURCE_AUDIO))
                .get()
                .extracting(reference -> reference.externalUrl())
                .isEqualTo("https://media.example/audio-repo-media-reference.ogg");
        assertThat(mediaReferenceRepository.findByOwnerAndPurpose(owner, MediaReferencePurpose.WHATSAPP_TRANSCODED_AUDIO))
                .get()
                .extracting(reference -> reference.storagePath())
                .isEqualTo("whatsapp/audio/audio-repo-media-reference.mp3");
    }
}
