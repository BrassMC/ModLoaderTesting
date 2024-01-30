package dev.turtywurty.testgradleplugin.tasks;

import dev.turtywurty.testgradleplugin.decompiler.VineflowerDecompiler;
import dev.turtywurty.testgradleplugin.extensions.TestGradleExtension;
import dev.turtywurty.testgradleplugin.piston.version.Library;
import org.gradle.api.tasks.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@CacheableTask
public class  DecompileTask extends DefaultTestGradleTask {
    @InputFile
    @Classpath
    private final Path inputJar, librariesJsonPath;

    private final Path outputDir;

    public DecompileTask() {
        Path cacheDir = getCacheDir();
        Path versionPath = cacheDir.resolve(getMinecraftVersion());

        this.inputJar = versionPath.resolve("recomp_" + switch (getSide()) {
            case CLIENT -> "client";
            case SERVER -> "server";
            case BOTH -> "joined";
        } + ".jar");
        this.librariesJsonPath = versionPath.resolve("libraries.json");

        this.outputDir = versionPath.resolve("decompiled_" + switch (getSide()) {
            case CLIENT -> "client";
            case SERVER -> "server";
            case BOTH -> "joined";
        });
    }

    @TaskAction
    public void decompileClient() {
        TestGradleExtension.Side side = getSide();

        System.out.printf("Decompiling %s for version %s%n", side.name().toLowerCase(Locale.ROOT), getMinecraftVersion());

        if (Files.notExists(inputJar))
            throw new IllegalStateException("Jar '%s' does not exist!".formatted(inputJar));

        if (Files.notExists(librariesJsonPath))
            throw new IllegalStateException("Libraries json '%s' does not exist!".formatted(librariesJsonPath));

        Map<String, Path> libraryJars = new HashMap<>();
        try {
            Library.readLibraries(libraryJars, librariesJsonPath);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read libraries json!", exception);
        }

        var decompiler = new VineflowerDecompiler(getProject());
        decompiler.decompile(inputJar, outputDir, libraryJars.values());
    }

    public Path getInputJar() {
        return inputJar;
    }

    public Path getLibrariesJsonPath() {
        return librariesJsonPath;
    }
}
