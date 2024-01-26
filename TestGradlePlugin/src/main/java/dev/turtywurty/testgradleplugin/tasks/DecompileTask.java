package dev.turtywurty.testgradleplugin.tasks;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.turtywurty.testgradleplugin.TestGradlePlugin;
import dev.turtywurty.testgradleplugin.extensions.TestGradleExtension;
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
import java.util.Locale;
import java.util.Map;

@CacheableTask
public abstract class DecompileTask extends DefaultTask {
    @Input
    public abstract Property<String> getVersion();

    @Input
    public abstract Property<String> getVineflowerVersion();

    @Input
    public abstract Property<TestGradleExtension.Side> getSide();

    @OutputDirectory
    @Optional
    public abstract DirectoryProperty getOutputDir();

    @TaskAction
    public void decompileClient() {
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

        TestGradleExtension.Side side = getSide().get();

        System.out.printf("Decompiling %s for version %s%n", side.name().toLowerCase(Locale.ROOT), getVersion().get());

        Path jarPath = versionPath.resolve(switch (side) {
            case CLIENT -> "client";
            case SERVER -> "server";
            case BOTH -> "joined";
        } + ".jar");
        if (Files.notExists(jarPath))
            throw new IllegalStateException("Jar '%s' does not exist!".formatted(jarPath));

        Path librariesJsonPath = versionPath.resolve("libraries.json");
        if (Files.notExists(librariesJsonPath))
            throw new IllegalStateException("Libraries json '%s' does not exist!".formatted(librariesJsonPath));

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
            throw new IllegalStateException("Failed to read libraries json!", exception);
        }

        var decompiler = new BaseDecompiler(
                new DirectoryResultSaver(versionPath.resolve("decompiled_" + switch (side) {
                    case CLIENT -> "client";
                    case SERVER -> "server";
                    case BOTH -> "joined";
                }).toFile()),
                new HashMap<>(),
                new PrintStreamLogger(System.out)
        );

        decompiler.addSource(jarPath.toFile());
        libraryJars.values().forEach(path -> decompiler.addLibrary(path.toFile()));

        decompiler.decompileContext();
    }
}
