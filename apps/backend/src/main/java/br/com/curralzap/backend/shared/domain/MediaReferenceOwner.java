package br.com.curralzap.backend.shared.domain;

import java.util.Objects;
import java.util.UUID;

public record MediaReferenceOwner(
        MediaReferenceOwnerType type,
        UUID id
) {

    public MediaReferenceOwner {
        Objects.requireNonNull(type, "Media reference owner type is required.");
        Objects.requireNonNull(id, "Media reference owner id is required.");
    }
}
