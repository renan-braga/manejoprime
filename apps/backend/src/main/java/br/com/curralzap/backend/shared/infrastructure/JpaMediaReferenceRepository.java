package br.com.curralzap.backend.shared.infrastructure;

import br.com.curralzap.backend.shared.application.MediaReferenceRepository;
import br.com.curralzap.backend.shared.domain.MediaReference;
import br.com.curralzap.backend.shared.domain.MediaReferenceOwner;
import br.com.curralzap.backend.shared.domain.MediaReferenceOwnerType;
import br.com.curralzap.backend.shared.domain.MediaReferencePurpose;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
class JpaMediaReferenceRepository implements MediaReferenceRepository {

    private final MediaReferenceJpaSpringRepository repository;

    JpaMediaReferenceRepository(MediaReferenceJpaSpringRepository repository) {
        this.repository = repository;
    }

    @Override
    public MediaReference save(MediaReference mediaReference) {
        MediaReferenceJpaEntity entity = repository.findByOwnerTypeAndOwnerIdAndPurpose(
                mediaReference.owner().type().databaseValue(),
                mediaReference.owner().id(),
                mediaReference.purpose().databaseValue()
        ).orElseGet(MediaReferenceJpaEntity::new);
        entity.setOwnerType(mediaReference.owner().type().databaseValue());
        entity.setOwnerId(mediaReference.owner().id());
        entity.setPurpose(mediaReference.purpose().databaseValue());
        entity.setExternalUrl(mediaReference.externalUrl());
        entity.setStoragePath(mediaReference.storagePath());
        entity.setMimeType(mediaReference.mimeType());
        entity.setFileSizeBytes(mediaReference.fileSizeBytes());
        entity.setChecksumSha256(mediaReference.checksumSha256());
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<MediaReference> findByOwnerAndPurpose(MediaReferenceOwner owner, MediaReferencePurpose purpose) {
        return repository.findByOwnerTypeAndOwnerIdAndPurpose(
                owner.type().databaseValue(),
                owner.id(),
                purpose.databaseValue()
        ).map(this::toDomain);
    }

    @Override
    public List<MediaReference> findByOwner(MediaReferenceOwner owner) {
        return repository.findByOwnerTypeAndOwnerIdOrderByPurposeAsc(
                owner.type().databaseValue(),
                owner.id()
        ).stream().map(this::toDomain).toList();
    }

    private MediaReference toDomain(MediaReferenceJpaEntity entity) {
        return new MediaReference(
                entity.getId(),
                new MediaReferenceOwner(
                        MediaReferenceOwnerType.fromDatabaseValue(entity.getOwnerType()),
                        entity.getOwnerId()
                ),
                MediaReferencePurpose.fromDatabaseValue(entity.getPurpose()),
                entity.getExternalUrl(),
                entity.getStoragePath(),
                entity.getMimeType(),
                entity.getFileSizeBytes(),
                entity.getChecksumSha256(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
