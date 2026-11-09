package dev.turtywurty.testgradleplugin.util;

import dev.turtywurty.testgradleplugin.HashingFunction;
import dev.turtywurty.testgradleplugin.piston.version.Download;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.io.InputStream;
import java.net.URLConnection;

public final class Downloads {
    private Downloads() {
    }

    public static Path resolveInside(Path root, String relative) {
        Path resolved = root.resolve(relative).normalize();
        if (!resolved.startsWith(root.normalize()))
            throw new IllegalArgumentException("Unsafe archive path: " + relative);

        return resolved;
    }

    public static boolean valid(Path path, Download download) {
        return Files.isRegularFile(path) && HashingFunction.SHA1.hash(path).equalsIgnoreCase(download.sha1());
    }

    public static void fetch(Download download, Path target) throws IOException {
        if (valid(target, download))
            return;

        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".part");
        try {
            URLConnection connection = URI.create(download.url()).toURL().openConnection();
            connection.setConnectTimeout(30_000);
            connection.setReadTimeout(60_000);
            try (InputStream input = connection.getInputStream()) {
                Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
            }

            if (Files.size(temporary) != download.size() || !valid(temporary, download))
                throw new IOException("Download checksum/size mismatch: " + download.url());

            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
