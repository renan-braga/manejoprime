package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.shared.application.MediaReferenceRepository;
import br.com.curralzap.backend.shared.domain.MediaReference;
import br.com.curralzap.backend.shared.domain.MediaReferenceOwner;
import br.com.curralzap.backend.shared.domain.MediaReferenceOwnerType;
import br.com.curralzap.backend.shared.domain.MediaReferencePurpose;
import br.com.curralzap.backend.whatsapp.application.WhatsappAudioMediaRepository;
import br.com.curralzap.backend.whatsapp.domain.WhatsappAudioMedia;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
class JpaWhatsappAudioMediaRepository implements WhatsappAudioMediaRepository {

    private final WhatsappAudioMediaJpaSpringRepository repository;
    private final MediaReferenceRepository mediaReferenceRepository;

    JpaWhatsappAudioMediaRepository(
            WhatsappAudioMediaJpaSpringRepository repository,
            MediaReferenceRepository mediaReferenceRepository
    ) {
        this.repository = repository;
        this.mediaReferenceRepository = mediaReferenceRepository;
    }

    @Override
    @Transactional
    public WhatsappAudioMedia save(WhatsappAudioMedia audioMedia) {
        WhatsappAudioMediaJpaEntity entity = audioMedia.id() == null
                ? new WhatsappAudioMediaJpaEntity()
                : repository.findById(audioMedia.id()).orElseGet(WhatsappAudioMediaJpaEntity::new);
        entity.setInboundMessageId(audioMedia.inboundMessageId());
        entity.setSourceUrl(audioMedia.sourceUrl());
        entity.setMimeType(audioMedia.mimeType());
        entity.setStoragePath(audioMedia.storagePath());
        entity.setDownloadedAt(audioMedia.downloadedAt());
        entity.setTranscodedMimeType(audioMedia.transcodedMimeType());
        entity.setTranscodedStoragePath(audioMedia.transcodedStoragePath());
        entity.setTranscodedAt(audioMedia.transcodedAt());
        WhatsappAudioMedia saved = toDomain(repository.save(entity));
        saveMediaReferences(saved);
        return saved;
    }

    @Override
    public Optional<WhatsappAudioMedia> findByInboundMessageId(UUID inboundMessageId) {
        return repository.findByInboundMessageId(inboundMessageId).map(this::toDomain);
    }

    private WhatsappAudioMedia toDomain(WhatsappAudioMediaJpaEntity entity) {
        return new WhatsappAudioMedia(
                entity.getId(),
                entity.getInboundMessageId(),
                entity.getSourceUrl(),
                entity.getMimeType(),
                entity.getStoragePath(),
                entity.getDownloadedAt(),
                entity.getTranscodedMimeType(),
                entity.getTranscodedStoragePath(),
                entity.getTranscodedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private void saveMediaReferences(WhatsappAudioMedia audioMedia) {
        MediaReferenceOwner owner = new MediaReferenceOwner(MediaReferenceOwnerType.WHATSAPP_AUDIO_MEDIA, audioMedia.id());
        if (audioMedia.sourceUrl() != null) {
            mediaReferenceRepository.save(MediaReference.externalUrl(
                    owner,
                    MediaReferencePurpose.WHATSAPP_SOURCE_AUDIO,
                    audioMedia.sourceUrl(),
                    audioMedia.mimeType(),
                    audioMedia.downloadedAt()
            ));
        }
        mediaReferenceRepository.save(MediaReference.storagePath(
                owner,
                MediaReferencePurpose.WHATSAPP_DOWNLOADED_AUDIO,
                audioMedia.storagePath(),
                audioMedia.mimeType(),
                null,
                null,
                audioMedia.downloadedAt()
        ));
        if (audioMedia.transcodedStoragePath() != null) {
            mediaReferenceRepository.save(MediaReference.storagePath(
                    owner,
                    MediaReferencePurpose.WHATSAPP_TRANSCODED_AUDIO,
                    audioMedia.transcodedStoragePath(),
                    audioMedia.transcodedMimeType(),
                    null,
                    null,
                    audioMedia.transcodedAt()
            ));
        }
    }
}
