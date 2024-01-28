package dev.turtywurty.testgradleplugin.tasks;

import dev.turtywurty.testgradleplugin.util.FileUtil;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.gradle.work.DisableCachingByDefault;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@DisableCachingByDefault(because = "It's unnecessary")
public abstract class ExtractClientTask extends DefaultTask {
    @Input
    public abstract Property<String> getVersion();

    @InputDirectory
    @Optional
    public abstract DirectoryProperty getInputDir();

    @TaskAction
    public void extractJar() {
        Path versionPath = getInputDir()
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

        Path jarPath = versionPath.resolve("client.jar");
        if (Files.notExists(jarPath))
            throw new IllegalStateException("Client jar does not exist!");

        Path outputDir = versionPath.resolve("client");
        try {
            if (Files.notExists(outputDir))
                Files.createDirectories(outputDir);
            else
                FileUtil.deleteDirectory(outputDir);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to create output directory!", exception);
        }

        long start = System.nanoTime();
        FileUtil.extractArchive(getProject(), jarPath, outputDir);
        System.out.println("Extracted client jar in " + (System.nanoTime() - start) / 1_000_000_000 + " seconds!");
    }
}
