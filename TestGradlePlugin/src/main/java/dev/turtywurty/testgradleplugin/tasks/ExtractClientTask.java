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

public abstract class ExtractClientTask extends DefaultTask {
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

        Path jarPath = versionPath.resolve("client.jar");
        if (Files.notExists(jarPath))
            throw new IllegalStateException("Client jar does not exist!");

        Path outputDir = versionPath.resolve("client");
        if (Files.exists(outputDir))
            ExtractServerTask.deleteDirectory(outputDir);

        try {
            Files.createDirectories(outputDir);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to create output directory!", exception);
        }

        // extract archive
        long start = System.nanoTime();
        ExtractServerTask.extractArchive(jarPath, outputDir);
        System.out.println("Extracted jar in " + (System.nanoTime() - start) / 1_000_000_000 + " seconds!");
    }
}
