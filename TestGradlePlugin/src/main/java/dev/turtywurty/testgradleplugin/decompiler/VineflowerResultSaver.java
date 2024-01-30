package dev.turtywurty.testgradleplugin.decompiler;

import org.jetbrains.java.decompiler.main.extern.IResultSaver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Manifest;

public class VineflowerResultSaver implements IResultSaver {
    private final Path outputDir;

    public VineflowerResultSaver(Path outputDir) {
        this.outputDir = outputDir;
    }

    @Override
    public void saveFolder(String path) {
        Path folder = outputDir.resolve(path);
        if(Files.notExists(folder)) {
            try {
                Files.createDirectories(folder);
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to create folder '%s'!".formatted(folder), exception);
            }
        }
    }

    @Override
    public void saveClassFile(String path, String qualifiedName, String entryName, String content, int[] mapping) {
        if (content == null)
            return;

        Path file = outputDir.resolve(path).resolve(entryName);
        try {
            if(Files.notExists(file.getParent())) {
                Files.createDirectories(file.getParent());
            }

            Files.writeString(file, content);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write file '%s'!".formatted(file), exception);
        }
    }

    @Override
    public void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName, String content) {
        saveClassFile(path, qualifiedName, entryName, content, null);
    }

    @Override
    public void copyFile(String source, String path, String entryName) {
        // NO-OP
    }

    @Override
    public void createArchive(String path, String archiveName, Manifest manifest) {
        // NO-OP
    }

    @Override
    public void saveDirEntry(String path, String archiveName, String entryName) {
        // NO-OP
    }

    @Override
    public void copyEntry(String source, String path, String archiveName, String entry) {
        // NO-OP
    }

    @Override
    public void closeArchive(String path, String archiveName) {
        // NO-OP
    }
}
