package dev.turtywurty.testgradleplugin.tasks;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.turtywurty.testgradleplugin.TestGradlePlugin;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.jetbrains.java.decompiler.main.decompiler.BaseDecompiler;
import org.jetbrains.java.decompiler.main.decompiler.DirectoryResultSaver;
import org.jetbrains.java.decompiler.main.decompiler.PrintStreamLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@CacheableTask
public abstract class DecompileClientTask extends DefaultTask {
    @Input
    public abstract Property<String> getVersion();

    @Input
    public abstract Property<String> getVineflowerVersion();

    @OutputDirectory
    @Optional
    public abstract DirectoryProperty getOutputDir();

    @TaskAction
    public void deobfuscateClient() {
        System.out.println("Decompiling client for version " + getVersion().get());

        Path versionPath = getOutputDir()
                .orElse(getProject()
                        .getLayout()
                        .getBuildDirectory()
                        .dir("minecraft")
                        .get())
                .get()
                .getAsFile()
                .toPath()
                .resolve(getVersion().get());

        Path clientJarPath = versionPath.resolve("client.jar");
        if (Files.notExists(clientJarPath)) {
            throw new RuntimeException("client.jar is missing, please run the downloadClient task!");
        }

        Path clientMappingsPath = versionPath.resolve("client_mappings.txt");
        if (Files.notExists(clientMappingsPath)) {
            throw new RuntimeException("client_mappings.txt is missing, please run the downloadClientMappings task!");
        }

        Path librariesJsonPath = versionPath.resolve("libraries.json");
        if (Files.notExists(librariesJsonPath)) {
            throw new RuntimeException("Libraries json does not exist!");
        }

        Map<String, Path> libraryJars = new HashMap<>();
        try {
            String librariesJson = Files.readString(librariesJsonPath);
            JsonObject librariesObject = TestGradlePlugin.GSON.fromJson(librariesJson, JsonObject.class);
            for (Map.Entry<String, JsonElement> entry : librariesObject.entrySet()) {
                String name = entry.getKey();
                String path = entry.getValue().getAsString();
                libraryJars.put(name, Path.of(path));
            }
        } catch (IOException exception) {
            throw new RuntimeException("Failed to read libraries json!", exception);
        }

        BaseDecompiler decompiler = new BaseDecompiler(
                new DirectoryResultSaver(versionPath.resolve("decompiled").toFile()),
                new HashMap<>(),
                new PrintStreamLogger(System.out)
        );

        decompiler.addSource(clientJarPath.toFile());
        libraryJars.values().forEach(path -> decompiler.addLibrary(path.toFile()));

        decompiler.decompileContext();
    }
}
