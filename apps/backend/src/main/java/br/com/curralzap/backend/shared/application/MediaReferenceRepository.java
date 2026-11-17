package br.com.curralzap.backend.shared.application;

import br.com.curralzap.backend.shared.domain.MediaReference;
import br.com.curralzap.backend.shared.domain.MediaReferenceOwner;
import br.com.curralzap.backend.shared.domain.MediaReferencePurpose;

import java.util.List;
import java.util.Optional;

public interface MediaReferenceRepository {

    MediaReference save(MediaReference mediaReference);

    Optional<MediaReference> findByOwnerAndPurpose(MediaReferenceOwner owner, MediaReferencePurpose purpose);

    List<MediaReference> findByOwner(MediaReferenceOwner owner);
}
