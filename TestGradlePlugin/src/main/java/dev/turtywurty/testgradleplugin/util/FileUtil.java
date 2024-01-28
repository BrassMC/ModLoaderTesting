package dev.turtywurty.testgradleplugin.util;

import org.gradle.api.Project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileUtil {
    public static void extractArchive(Path archiveFile, Path destPath) {
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

    public static void extractArchive(Project project, Path archiveFile, Path destPath) {
        project.copy(copySpec -> {
            copySpec.from(project.zipTree(archiveFile.toFile()));
            copySpec.into(destPath.toFile());
        });
    }

    public static void deleteDirectory(Path directory) {
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

    public static void moveFiles(Path inputDir, Path outputDir) {
        try (var paths = Files.walk(inputDir)) {
            paths.forEach(path -> {
                try {
                    Files.move(path, outputDir.resolve(inputDir.relativize(path)));
                } catch (IOException exception) {
                    throw new IllegalStateException("Failed to move file!", exception);
                }
            });
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to move files!", exception);
        }
    }
}
