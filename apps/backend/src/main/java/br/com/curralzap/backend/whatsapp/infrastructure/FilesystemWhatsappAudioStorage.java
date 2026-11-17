package br.com.curralzap.backend.whatsapp.infrastructure;

import br.com.curralzap.backend.whatsapp.application.StoreWhatsappAudioRequest;
import br.com.curralzap.backend.whatsapp.application.WhatsappAudioStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Component
class FilesystemWhatsappAudioStorage implements WhatsappAudioStorage {

    private final Path storageRoot;

    FilesystemWhatsappAudioStorage(
            @Value("${curralzap.whatsapp.audio.storage-root:}") String configuredStorageRoot
    ) {
        this.storageRoot = configuredStorageRoot == null || configuredStorageRoot.isBlank()
                ? Path.of(System.getProperty("java.io.tmpdir"), "curralzap", "whatsapp-audio")
                : Path.of(configuredStorageRoot);
    }

    @Override
    public String store(StoreWhatsappAudioRequest request) {
        try {
            Files.createDirectories(storageRoot);
            String fileName = request.storageKey() + extensionFor(request.mimeType());
            Path target = storageRoot.resolve(fileName);
            Files.write(target, request.content(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return "whatsapp/audio/" + fileName;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to store downloaded audio.", exception);
        }
    }

    @Override
    public byte[] load(String storagePath) {
        try {
            return Files.readAllBytes(resolve(storagePath));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load stored audio.", exception);
        }
    }

    private Path resolve(String storagePath) {
        String relativePath = storagePath;
        if (relativePath.startsWith("whatsapp/audio/")) {
            relativePath = relativePath.substring("whatsapp/audio/".length());
        }
        return storageRoot.resolve(relativePath);
    }

    private String extensionFor(String mimeType) {
        if (mimeType == null) {
            return ".bin";
        }
        return switch (mimeType) {
            case "audio/ogg", "audio/ogg; codecs=opus" -> ".ogg";
            case "audio/opus" -> ".opus";
            case "audio/mpeg" -> ".mp3";
            case "audio/wav", "audio/x-wav" -> ".wav";
            default -> ".bin";
        };
    }
}
