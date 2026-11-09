package dev.turtywurty.testgradleplugin.tasks;

import com.google.gson.JsonObject;
import dev.turtywurty.testgradleplugin.HashingFunction;
import dev.turtywurty.testgradleplugin.TestGradlePlugin;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import com.google.gson.JsonElement;
import java.io.InputStream;
import java.nio.file.Path;

@DisableCachingByDefault(because = "Prepares locally cached Minecraft artifacts")
public abstract class DownloadPistonMetaTask extends DefaultTask {
    @Input
    public abstract Property<String> getMinecraftVersion();
    @Input
    public abstract Property<String> getManifestUrl();
    @OutputFile
    public abstract RegularFileProperty getVersionJson();

    @TaskAction
    public void download() throws IOException {
        JsonObject manifest;
        try (InputStream input = URI.create(getManifestUrl().get()).toURL().openStream()) {
            manifest = TestGradlePlugin.GSON.fromJson(new String(input.readAllBytes(), StandardCharsets.UTF_8),
                JsonObject.class);
        }

        for (JsonElement element : manifest.getAsJsonArray("versions")) {
            JsonObject version = element.getAsJsonObject();
            if (version.get("id").getAsString().equals(getMinecraftVersion().get())) {
                byte[] bytes;
                try (InputStream input = URI.create(version.get("url").getAsString()).toURL().openStream()) {
                    bytes = input.readAllBytes();
                }

                if (!HashingFunction.SHA1.hash(bytes).equals(version.get("sha1").getAsString()))
                    throw new IOException("Version metadata checksum mismatch");

                Path output = getVersionJson().get().getAsFile().toPath();
                Files.createDirectories(output.getParent());
                Files.write(output, bytes);
                return;
            }
        }

        throw new IllegalArgumentException("Minecraft version not found: " + getMinecraftVersion().get());
    }
}
