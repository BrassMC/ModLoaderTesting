package dev.turtywurty.testgradleplugin.tasks;

import dev.turtywurty.testgradleplugin.extensions.TestGradleExtension;
import org.gradle.api.tasks.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

@CacheableTask
public class RepackageTask extends DefaultTestGradleTask {
    @InputDirectory
    @Classpath
    private final Path inputDir;

    @OutputFile
    private final Path outputJar;

    public RepackageTask() {
        Path cacheDir = getCacheDir();
        Path versionPath = cacheDir.resolve(getMinecraftVersion());

        TestGradleExtension.Side side = getSide();
        this.inputDir = versionPath.resolve("remapped_" + switch (side) {
            case CLIENT -> "client";
            case SERVER -> "server";
            case BOTH -> "joined";
        });

        this.outputJar = versionPath.resolve("repackaged_" + switch (side) {
            case CLIENT -> "client";
            case SERVER -> "server";
            case BOTH -> "joined";
        } + ".jar");
    }

    @TaskAction
    public void repackage() {
        TestGradleExtension.Side side = getSide();
        if (Files.notExists(inputDir))
            throw new IllegalStateException("The " + side.name().toLowerCase() + " has not been extracted yet!");

        System.out.println("Repackaging " + side.name().toLowerCase() + " for version " + getMinecraftVersion() + "...");

        try {
            Files.deleteIfExists(outputJar);
            Files.createDirectories(outputJar.getParent());
            Files.createFile(outputJar);

            try (var fos = new FileOutputStream(outputJar.toFile());
                 var jos = new JarOutputStream(fos);
                 Stream<Path> walk = Files.walk(inputDir)) {
                walk.filter(Files::isRegularFile).forEach(path -> {
                    try {
                        var entry = new JarEntry(inputDir.relativize(path).toString());
                        jos.putNextEntry(entry);
                        jos.write(Files.readAllBytes(path));
                        jos.closeEntry();

                        //System.out.println("Added " + entry.getName() + " to " + outputJar.getFileName());
                    } catch (IOException exception) {
                        throw new IllegalStateException("Failed to add " + path.getFileName() + " to " + outputJar.getFileName(), exception);
                    }
                });
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to delete old jar!", exception);
        }

        System.out.println("Successfully repackaged " + side.name().toLowerCase() + " for version " + getMinecraftVersion() + "!");

        // add the recompiled jar to the classpath
        getProject().getRepositories().flatDir(repo -> repo.dir(outputJar.getParent()));
        getProject().getDependencies().add("implementation", getProject().files(outputJar));
    }

    public Path getInputDir() {
        return inputDir;
    }

    public Path getOutputJar() {
        return outputJar;
    }
}
