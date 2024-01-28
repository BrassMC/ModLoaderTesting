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
public abstract class ExtractServerTask extends DefaultTask {
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

        Path jarPath = versionPath.resolve("server.jar");
        if (Files.notExists(jarPath))
            throw new IllegalStateException("Server jar does not exist!");

        // get or else use user home dir
        Path outputDir = versionPath.resolve("server");
        if (Files.exists(outputDir)) {
            // delete output dir
            FileUtil.deleteDirectory(outputDir);
        } else {
            try {
                Files.createDirectories(outputDir);
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to create output directory!", exception);
            }
        }

        long start = System.nanoTime();
        FileUtil.extractArchive(getProject(), jarPath, outputDir);

        if (Files.exists(outputDir.resolve("META-INF/versions/%s/server-%s.jar"))) {
            // clear output dir
            Path tempOutputDir = versionPath.resolve("temp-server");
            try {
                Files.createDirectories(tempOutputDir);
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to create output directory!", exception);
            }

            // move files to temp output dir
            FileUtil.moveFiles(outputDir, tempOutputDir);

            start = System.nanoTime();
            FileUtil.extractArchive(getProject(), tempOutputDir.resolve("META-INF/versions/%s/server-%s.jar"), outputDir);

            // delete temp output dir
            FileUtil.deleteDirectory(tempOutputDir);
        }

        System.out.println("Extracted server jar(s) in " + (System.nanoTime() - start) / 1_000_000_000 + " seconds!");
    }
}
