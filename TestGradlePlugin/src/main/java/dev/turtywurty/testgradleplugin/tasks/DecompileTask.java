package dev.turtywurty.testgradleplugin.tasks;

import dev.turtywurty.testgradleplugin.extensions.TestGradleExtension;
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
public class DecompileTask extends DefaultTestGradleTask {
    @InputFile
    @Classpath
    private final Path jarPath, librariesJsonPath;

    private final Path outputDir;

    public DecompileTask() {
        Path cacheDir = getCacheDir();
        this.outputDir = cacheDir.resolve("decompiled_" + switch (getSide()) {
            case CLIENT -> "client";
            case SERVER -> "server";
            case BOTH -> "joined";
        });

        Path versionPath = cacheDir.resolve(getMinecraftVersion());

        this.jarPath = versionPath.resolve(switch (getSide()) {
            case CLIENT -> "client";
            case SERVER -> "server";
            case BOTH -> "joined";
        } + ".jar");

        this.librariesJsonPath = versionPath.resolve("libraries.json");
    }

    @TaskAction
    public void decompileClient() {
        TestGradleExtension.Side side = getSide();

        System.out.printf("Decompiling %s for version %s%n", side.name().toLowerCase(Locale.ROOT), getMinecraftVersion());

        if (Files.notExists(jarPath))
            throw new IllegalStateException("Jar '%s' does not exist!".formatted(jarPath));

        if (Files.notExists(librariesJsonPath))
            throw new IllegalStateException("Libraries json '%s' does not exist!".formatted(librariesJsonPath));

        Map<String, Path> libraryJars = new HashMap<>();
        try {
            RunClientTask.readLibraries(libraryJars, librariesJsonPath);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read libraries json!", exception);
        }

        var decompiler = new BaseDecompiler(
                new DirectoryResultSaver(outputDir.toFile()),
                new HashMap<>(),
                new PrintStreamLogger(System.out)
        );

        decompiler.addSource(jarPath.toFile());
        libraryJars.values().forEach(path -> decompiler.addLibrary(path.toFile()));

        decompiler.decompileContext();
    }

    public Path getJarPath() {
        return jarPath;
    }

    public Path getLibrariesJsonPath() {
        return librariesJsonPath;
    }
}
