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

    /**
     * If the file consists of the bytes 0x09 (tab), 0x0A (line feed), 0x0C (form feed), 0x0D (carriage return), or 0x20 through 0x7E, then it's probably ASCII text.
     * <p>
     * If the file contains any other ASCII control character, 0x00 through 0x1F excluding the three above, then it's probably binary data.
     */
    public static boolean isBinaryFile(Path path) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            for (byte b : bytes) {
                if (b < 0x09 || b == 0x0B || b == 0x0E || b == 0x0F || b == 0x10 || b == 0x11 ||
                        b == 0x12 || b == 0x13 || b == 0x14 || b == 0x15 || b == 0x16 ||
                        b == 0x17 || b == 0x18 || b == 0x19 || b == 0x1A || b == 0x1B ||
                        b == 0x1C || b == 0x1D || b == 0x1E || b == 0x1F || b == 0x7F) {
                    return true;
                }
            }
            return false;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to check if file '%s' is binary!".formatted(path), exception);
        }
    }
}
