package dev.turtywurty.testgradleplugin.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

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

        Path outputDir = versionPath.resolve("server");
        if (Files.exists(outputDir))
            try (Stream<Path> paths = Files.walk(outputDir)) {
                paths.forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException exception) {
                        throw new IllegalStateException("Failed to delete path!", exception);
                    }
                });
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to walk directory!", exception);
            }
        else
            try {
                Files.createDirectories(outputDir);
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to create output directory!", exception);
            }

        // extract jar
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                Path entryPath = outputDir.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(jar.getInputStream(entry), entryPath);
                }
            }

            System.out.println("Extracted jar!");
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to extract jar!", exception);
        }
    }
}
