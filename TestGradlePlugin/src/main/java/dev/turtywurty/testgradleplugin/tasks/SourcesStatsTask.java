package dev.turtywurty.testgradleplugin.tasks;

import dev.turtywurty.testgradleplugin.util.FileUtil;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class SourcesStatsTask extends DefaultTestGradleTask {
    @InputDirectory
    @Classpath
    private final Path decompiledPath;

    public SourcesStatsTask() {
        Path cacheDir = getCacheDir();
        Path versionPath = cacheDir.resolve(getMinecraftVersion());

        this.decompiledPath = versionPath.resolve("decompiled_" + switch (getSide()) {
            case CLIENT -> "client";
            case SERVER -> "server";
            case BOTH -> "joined";
        });
    }

    @TaskAction
    public void run() {
        System.out.println("Version: " + getMinecraftVersion());
        System.out.println("Side: " + getSide());

        if (Files.notExists(decompiledPath))
            throw new IllegalStateException("Decompiled path '%s' does not exist!".formatted(decompiledPath));

        System.out.println("Decompiled Path: " + decompiledPath);

        var count = new AtomicInteger(0);
        var javaCount = new AtomicInteger(0);
        var javaLineCount = new AtomicInteger(0);
        var javaLineNoWhitespaceCount = new AtomicInteger(0);
        var lineCount = new AtomicInteger(0);
        var lineNoWhitespaceCount = new AtomicInteger(0);
        var charCount = new AtomicLong(0);
        try (var stream = Files.walk(decompiledPath)) {
            stream.filter(Files::isRegularFile)
                    .forEach(path -> {
                        count.incrementAndGet();
                        if (path.toString().endsWith(".java")) {
                            javaCount.incrementAndGet();
                            try {
                                List<String> lines = Files.readAllLines(path);
                                javaLineCount.addAndGet(lines.size());
                                lineCount.addAndGet(lines.size());
                                lines.forEach(line -> {
                                    if (!line.isBlank()) {
                                        javaLineNoWhitespaceCount.incrementAndGet();
                                        lineNoWhitespaceCount.incrementAndGet();
                                        charCount.addAndGet(line.length());
                                    }
                                });
                            } catch (IOException exception) {
                                throw new IllegalStateException("Failed to count lines in file '%s'!".formatted(path), exception);
                            }
                        } else if (!FileUtil.isBinaryFile(path)) {
                            try {
                                List<String> lines = Files.readAllLines(path);
                                lineCount.addAndGet(lines.size());
                                lines.forEach(line -> {
                                    if (!line.isBlank()) {
                                        lineNoWhitespaceCount.incrementAndGet();
                                        charCount.addAndGet(line.length());
                                    }
                                });
                            } catch (IOException exception) {
                                throw new IllegalStateException("Failed to count lines in file '%s'!".formatted(path), exception);
                            }
                        }
                    });
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to count files in decompiled path!", exception);
        }

        System.out.println("Java File Count: " + javaCount.get());
        System.out.println("Java Line Count: " + javaLineCount.get());
        System.out.println("Java Line No Whitespace Count: " + javaLineNoWhitespaceCount.get());
        System.out.println("File Count: " + count.get());
        System.out.println("Line Count: " + lineCount.get());
        System.out.println("Line No Whitespace Count: " + lineNoWhitespaceCount.get());
        System.out.println("Char Count: " + charCount.get());
    }

    public Path getDecompiledPath() {
        return decompiledPath;
    }
}
