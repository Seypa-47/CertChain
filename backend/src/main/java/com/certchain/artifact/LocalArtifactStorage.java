package com.certchain.artifact;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.storage", name = "type", havingValue = "local", matchIfMissing = true)
public class LocalArtifactStorage implements ArtifactStorage {
    private static final Pattern KEY = Pattern.compile("certificates/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/certificate\\.pdf");
    private static final int MAX_BYTES = 10 * 1024 * 1024;
    private final Path root;

    public LocalArtifactStorage(@Value("${app.storage.root}") String configuredRoot) {
        try {
            Path configured = Path.of(configuredRoot).toAbsolutePath().normalize();
            Files.createDirectories(configured);
            this.root = configured.toRealPath();
        } catch (IOException error) {
            throw new IllegalStateException("Artifact storage is unavailable", error);
        }
    }

    private Path path(String key) {
        if (key == null || !KEY.matcher(key).matches()) throw new IllegalArgumentException("Invalid artifact key");
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) throw new IllegalArgumentException("Invalid artifact key");
        Path parent = target.getParent();
        try {
            Path namespace = root.resolve("certificates");
            if (Files.isSymbolicLink(namespace)) throw new IllegalArgumentException("Unsafe artifact path");
            Files.createDirectories(namespace);
            if (!namespace.toRealPath().equals(namespace)) throw new IllegalArgumentException("Unsafe artifact path");
            if (Files.isSymbolicLink(parent)) throw new IllegalArgumentException("Unsafe artifact path");
            Files.createDirectories(parent);
            if (!parent.toRealPath().startsWith(namespace)
                || Files.isSymbolicLink(target)) throw new IllegalArgumentException("Unsafe artifact path");
        } catch (IOException error) {
            throw new IllegalStateException("Artifact storage is unavailable", error);
        }
        return target;
    }

    @Override
    public void write(String key, byte[] content) {
        if (content == null || content.length == 0 || content.length > MAX_BYTES)
            throw new IllegalArgumentException("Invalid artifact size");
        Path target = path(key);
        Path temporary = null;
        try {
            temporary = Files.createTempFile(target.getParent(), "certificate-", ".tmp");
            Files.write(temporary, content);
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException error) {
            throw new IllegalStateException("Artifact write failed", error);
        } finally {
            if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
        }
    }

    @Override
    public byte[] read(String key) {
        Path target = path(key);
        try {
            if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS) || Files.size(target) > MAX_BYTES)
                throw new IllegalStateException("Artifact is unavailable");
            return Files.readAllBytes(target);
        } catch (IOException error) {
            throw new IllegalStateException("Artifact read failed", error);
        }
    }

    @Override
    public boolean exists(String key) {
        Path target = path(key);
        return Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS);
    }
}
