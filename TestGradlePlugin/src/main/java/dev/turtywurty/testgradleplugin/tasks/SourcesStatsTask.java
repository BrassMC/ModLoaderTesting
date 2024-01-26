package dev.turtywurty.testgradleplugin.tasks;

import dev.turtywurty.testgradleplugin.extensions.TestGradleExtension;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public abstract class SourcesStatsTask extends DefaultTask {
    @Input
    public abstract Property<String> getVersion();

    @Input
    public abstract Property<TestGradleExtension.Side> getSide();

    @OutputDirectory
    @Optional
    public abstract DirectoryProperty getOutputDir();

    @TaskAction
    public void run() {
        System.out.println("Version: " + getVersion().get());
        System.out.println("Side: " + getSide().get());
        System.out.println("Output Dir: " + getOutputDir().getOrNull());

        Path versionPath = getOutputDir()
                .orElse(getProject()
                        .getLayout()
                        .getBuildDirectory()
                        .dir("minecraft")
                        .get())
                .get()
                .getAsFile()
                .toPath()
                .resolve(getVersion().get());

        System.out.println("Version Path: " + versionPath);

        TestGradleExtension.Side side = getSide().get();

        Path decompiledPath = versionPath.resolve("decompiled_" + switch (side) {
            case CLIENT -> "client";
            case SERVER -> "server";
            case BOTH -> "joined";
        });

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
        try {
            Files.walk(decompiledPath)
                    .filter(Files::isRegularFile)
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
                        } else if (!path.toString().endsWith(".png") &&
                                !path.toString().endsWith(".nbt") &&
                                !path.toString().endsWith(".RSA") &&
                                !path.toString().endsWith(".MF") &&
                                !path.toString().endsWith(".SF") &&
                                !path.toString().endsWith(".DSA")) {
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
}
