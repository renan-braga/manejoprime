package br.com.curralzap.backend.shared.domain;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public record MediaReference(
        UUID id,
        MediaReferenceOwner owner,
        MediaReferencePurpose purpose,
        String externalUrl,
        String storagePath,
        String mimeType,
        Long fileSizeBytes,
        String checksumSha256,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    private static final int MAX_EXTERNAL_URL_LENGTH = 1000;
    private static final int MAX_STORAGE_PATH_LENGTH = 500;
    private static final int MAX_MIME_TYPE_LENGTH = 100;
    private static final int CHECKSUM_SHA256_LENGTH = 64;

    public MediaReference {
        Objects.requireNonNull(owner, "Media reference owner is required.");
        Objects.requireNonNull(purpose, "Media reference purpose is required.");
        externalUrl = trimToNull(externalUrl);
        storagePath = trimToNull(storagePath);
        mimeType = trimToNull(mimeType);
        checksumSha256 = trimToNull(checksumSha256);
        validateLocation(externalUrl, storagePath);
        validateLength(externalUrl, MAX_EXTERNAL_URL_LENGTH, "Media reference external URL");
        validateLength(storagePath, MAX_STORAGE_PATH_LENGTH, "Media reference storage path");
        validateLength(mimeType, MAX_MIME_TYPE_LENGTH, "Media reference MIME type");
        validateFileSize(fileSizeBytes);
        validateChecksum(checksumSha256);
    }

    public static MediaReference externalUrl(
            MediaReferenceOwner owner,
            MediaReferencePurpose purpose,
            String externalUrl,
            String mimeType,
            OffsetDateTime referencedAt
    ) {
        return new MediaReference(
                null,
                owner,
                purpose,
                externalUrl,
                null,
                mimeType,
                null,
                null,
                referencedAt,
                referencedAt
        );
    }

    public static MediaReference storagePath(
            MediaReferenceOwner owner,
            MediaReferencePurpose purpose,
            String storagePath,
            String mimeType,
            Long fileSizeBytes,
            String checksumSha256,
            OffsetDateTime referencedAt
    ) {
        return new MediaReference(
                null,
                owner,
                purpose,
                null,
                storagePath,
                mimeType,
                fileSizeBytes,
                checksumSha256,
                referencedAt,
                referencedAt
        );
    }

    public MediaReference withIdAndCreatedAt(UUID id, OffsetDateTime createdAt) {
        return new MediaReference(
                id,
                owner,
                purpose,
                externalUrl,
                storagePath,
                mimeType,
                fileSizeBytes,
                checksumSha256,
                createdAt,
                updatedAt
        );
    }

    private static void validateLocation(String externalUrl, String storagePath) {
        if ((externalUrl == null && storagePath == null) || (externalUrl != null && storagePath != null)) {
            throw new IllegalArgumentException("Media reference must have exactly one location.");
        }
        if (externalUrl != null) {
            validateExternalUrl(externalUrl);
        }
    }

    private static void validateExternalUrl(String externalUrl) {
        try {
            URI uri = new URI(externalUrl);
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw new IllegalArgumentException("Media reference external URL must use http or https.");
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new IllegalArgumentException("Media reference external URL must include a host.");
            }
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Media reference external URL must be a valid URI.", exception);
        }
    }

    private static void validateLength(String value, int maxLength, String fieldName) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " must be at most " + maxLength + " characters.");
        }
    }

    private static void validateFileSize(Long fileSizeBytes) {
        if (fileSizeBytes != null && fileSizeBytes < 0) {
            throw new IllegalArgumentException("Media reference file size must be non-negative.");
        }
    }

    private static void validateChecksum(String checksumSha256) {
        if (checksumSha256 != null && checksumSha256.length() != CHECKSUM_SHA256_LENGTH) {
            throw new IllegalArgumentException("Media reference SHA-256 checksum must be 64 characters.");
        }
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
