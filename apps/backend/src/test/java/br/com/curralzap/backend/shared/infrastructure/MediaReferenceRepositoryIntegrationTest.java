package br.com.curralzap.backend.shared.infrastructure;

import br.com.curralzap.backend.shared.application.MediaReferenceRepository;
import br.com.curralzap.backend.shared.domain.MediaReference;
import br.com.curralzap.backend.shared.domain.MediaReferenceOwner;
import br.com.curralzap.backend.shared.domain.MediaReferenceOwnerType;
import br.com.curralzap.backend.shared.domain.MediaReferencePurpose;
import br.com.curralzap.backend.support.BackendIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MediaReferenceRepositoryIntegrationTest extends BackendIntegrationTestSupport {

    @Autowired
    private MediaReferenceRepository repository;

    @Test
    void savesOccurrencePhotoExternalUrlWithoutBinaryContent() {
        UUID farmId = insertFarm("Fazenda Midia Ocorrencia");
        UUID animalId = insertAnimal(farmId, null, "MID-001");
        UUID userId = insertUser(farmId, "5511999998100", "Midia User", "worker", true).id();
        UUID occurrenceId = insertOccurrenceRecord(farmId, animalId, userId, "open", null);
        OffsetDateTime capturedAt = OffsetDateTime.parse("2026-04-28T12:00:00Z");

        MediaReference saved = repository.save(MediaReference.externalUrl(
                new MediaReferenceOwner(MediaReferenceOwnerType.OCCURRENCE, occurrenceId),
                MediaReferencePurpose.OCCURRENCE_PHOTO,
                "https://media.example/occurrences/photo-001.jpg",
                "image/jpeg",
                capturedAt
        ));

        assertThat(saved.id()).isNotNull();
        assertThat(repository.findByOwnerAndPurpose(saved.owner(), MediaReferencePurpose.OCCURRENCE_PHOTO))
                .get()
                .extracting(
                        MediaReference::externalUrl,
                        MediaReference::storagePath,
                        MediaReference::mimeType
                )
                .containsExactly(
                        "https://media.example/occurrences/photo-001.jpg",
                        null,
                        "image/jpeg"
                );
    }

    @Test
    void savesWhatsappStoredAudioMetadataWithoutBinaryContent() {
        UUID inboundMessageId = insertWhatsappInboundMessage(
                "wamid.media-reference-audio",
                "5511999998101",
                null,
                null,
                "corr-media-reference-audio",
                "messages.upsert",
                "audioMessage",
                "RECEIVED",
                null
        );
        UUID audioMediaId = UUID.randomUUID();
        OffsetDateTime downloadedAt = OffsetDateTime.parse("2026-04-28T12:05:00Z");

        MediaReference saved = repository.save(MediaReference.storagePath(
                new MediaReferenceOwner(MediaReferenceOwnerType.WHATSAPP_AUDIO_MEDIA, audioMediaId),
                MediaReferencePurpose.WHATSAPP_DOWNLOADED_AUDIO,
                "whatsapp/audio/wamid.media-reference-audio.ogg",
                "audio/ogg",
                4096L,
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                downloadedAt
        ));

        assertThat(inboundMessageId).isNotNull();
        assertThat(repository.findByOwner(new MediaReferenceOwner(MediaReferenceOwnerType.WHATSAPP_AUDIO_MEDIA, audioMediaId)))
                .singleElement()
                .extracting(
                        MediaReference::purpose,
                        MediaReference::externalUrl,
                        MediaReference::storagePath,
                        MediaReference::fileSizeBytes,
                        MediaReference::checksumSha256
                )
                .containsExactly(
                        MediaReferencePurpose.WHATSAPP_DOWNLOADED_AUDIO,
                        null,
                        "whatsapp/audio/wamid.media-reference-audio.ogg",
                        4096L,
                        "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
                );
        assertThat(saved.id()).isNotNull();
    }

    @Test
    void replacesReferenceForSameOwnerAndPurpose() {
        MediaReferenceOwner owner = new MediaReferenceOwner(MediaReferenceOwnerType.OCCURRENCE, UUID.randomUUID());

        MediaReference first = repository.save(MediaReference.externalUrl(
                owner,
                MediaReferencePurpose.OCCURRENCE_PHOTO,
                "https://media.example/occurrences/old.jpg",
                "image/jpeg",
                OffsetDateTime.parse("2026-04-28T12:10:00Z")
        ));
        MediaReference second = repository.save(MediaReference.externalUrl(
                owner,
                MediaReferencePurpose.OCCURRENCE_PHOTO,
                "https://media.example/occurrences/new.jpg",
                "image/jpeg",
                OffsetDateTime.parse("2026-04-28T12:11:00Z")
        ));

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(repository.findByOwner(owner))
                .singleElement()
                .extracting(MediaReference::externalUrl)
                .isEqualTo("https://media.example/occurrences/new.jpg");
    }

    @Test
    void rejectsNonHttpExternalUrls() {
        MediaReferenceOwner owner = new MediaReferenceOwner(MediaReferenceOwnerType.OCCURRENCE, UUID.randomUUID());

        assertThatThrownBy(() -> MediaReference.externalUrl(
                owner,
                MediaReferencePurpose.OCCURRENCE_PHOTO,
                "ftp://media.example/photo.jpg",
                "image/jpeg",
                OffsetDateTime.parse("2026-04-28T12:15:00Z")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Media reference external URL must use http or https.");
    }
}
