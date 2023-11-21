package dev.turtywurty.testgradleplugin.piston.version;

import dev.turtywurty.testgradleplugin.TestGradlePlugin;
import org.gradle.internal.impldep.com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public record Download(String sha1, long size, String url) {
    public static Download fromJson(JsonObject json) {
        return TestGradlePlugin.GSON.fromJson(json, Download.class);
    }

    public void downloadToPath(Path path) {
        String[] split = this.url.split("/");
        String fileName = split[split.length - 1];

        downloadToPath(path, fileName);
    }

    public void downloadToPath(Path path, String fileName) {
        Path resolved = path.resolve(fileName);
        System.out.println("Downloading " + this.url + " to " + resolved + "...");

        try {
            Files.createDirectories(resolved.getParent());
            Files.deleteIfExists(resolved);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to create directories for " + resolved + "!", exception);
        }

        try (InputStream inputStream = new URL(this.url).openStream()) {
            Files.write(resolved, inputStream.readAllBytes());
        } catch (IOException exception) {
            throw new RuntimeException("Failed to download " + this.url + "!", exception);
        }
    }
}