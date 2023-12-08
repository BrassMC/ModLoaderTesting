package dev.turtywurty.testgradleplugin.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@CacheableTask
public abstract class ExtractServerTask extends DefaultTask {
    @Input
    public abstract Property<String> getVersion();

    @OutputDirectory
    @Optional
    public abstract DirectoryProperty getOutputDir();

    @TaskAction
    public void extractJar() {
        System.out.println("Extracting jar!");

        Path versionPath = getOutputDir()
                .getOrElse(getProject()
                        .getLayout()
                        .getBuildDirectory()
                        .dir("minecraft")
                        .get())
                .getAsFile()
                .toPath()
                .resolve(getVersion().get());
        if (Files.notExists(versionPath))
            throw new IllegalStateException("Version directory does not exist!");

        Path jarPath = versionPath.resolve("server.jar");
        if (Files.notExists(jarPath))
            throw new IllegalStateException("Server jar does not exist!");

        Path tempOutputDir = versionPath.resolve("temp-server");
        try {
            Files.createDirectories(tempOutputDir);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to create output directory!", exception);
        }

        // extract jar
        extractJar(jarPath, tempOutputDir);

        Path serverJar = tempOutputDir.resolve("META-INF/versions/%s/server-%s.jar"
                .formatted(getVersion().get(), getVersion().get())); // TODO: Read from versions.list
        if (Files.notExists(serverJar))
            throw new IllegalStateException("Server jar does not exist!");

        Path outputDir = versionPath.resolve("server");

        // extract server jar
        extractJar(serverJar, outputDir);

        // delete temp output dir
        try {
            Files.walk(tempOutputDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException exception) {
                            throw new IllegalStateException("Failed to delete file!", exception);
                        }
                    });
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to delete temp output directory!", exception);
        }
    }

    private static void extractJar(Path jarPath, Path outputDir) {
        try (var jar = new JarFile(jarPath.toFile())) {
            int extractedFiles = 0, extractedDirs = 0;
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                Path destination = outputDir.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(destination);
                    extractedDirs++;
                } else {
                    Files.createDirectories(destination.getParent());
                    Files.copy(jar.getInputStream(entry), destination);
                    extractedFiles++;
                }
            }

            System.out.printf("Extracted %d files and %d directories!%n", extractedFiles, extractedDirs);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to extract jar!", exception);
        }
    }
}
