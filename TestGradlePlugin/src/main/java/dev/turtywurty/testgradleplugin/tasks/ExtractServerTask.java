package dev.turtywurty.testgradleplugin.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public abstract class ExtractServerTask extends DefaultTask {
    static void deleteDirectory(Path directory) {
        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to delete directory: " + directory, exception);
        }
    }

    static void extractArchive(Path archiveFile, Path destPath) {
        try (var zipIn = new ZipInputStream(Files.newInputStream(archiveFile))) {
            Files.createDirectories(destPath);

            ZipEntry entry = zipIn.getNextEntry();
            while (entry != null) {
                Path filePath = destPath.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(filePath);
                } else if (Files.notExists(filePath.getParent())) {
                    Files.createDirectories(filePath.getParent());
                }

                Files.copy(zipIn, filePath);

                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to extract archive: " + archiveFile, exception);
        }
    }

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
        if (Files.exists(outputDir)) {
            // delete output dir
            deleteDirectory(outputDir);
        } else {
            try {
                Files.createDirectories(outputDir);
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to create output directory!", exception);
            }
        }

        extractArchive(jarPath, outputDir);

        if (Files.exists(outputDir.resolve("META-INF/versions/%s/server-%s.jar"))) {
            // clear output dir
            Path tempOutputDir = versionPath.resolve("temp-server");
            try {
                Files.createDirectories(tempOutputDir);
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to create output directory!", exception);
            }

            // move files to temp output dir
            try (var paths = Files.walk(outputDir)) {
                paths.forEach(path -> {
                    try {
                        Files.move(path, tempOutputDir.resolve(outputDir.relativize(path)));
                    } catch (IOException exception) {
                        throw new IllegalStateException("Failed to move file!", exception);
                    }
                });
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to move files!", exception);
            }

            extractArchive(tempOutputDir.resolve("META-INF/versions/%s/server-%s.jar"), outputDir);

            // delete temp output dir
            deleteDirectory(tempOutputDir);
        }

        System.out.println("Finished extracting jar!");
    }
}
