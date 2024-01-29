package dev.turtywurty.testgradleplugin.tasks;

import dev.turtywurty.testgradleplugin.util.FileUtil;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@DisableCachingByDefault(because = "It's unnecessary")
public class ExtractServerTask extends DefaultTestGradleTask {
    @InputFile
    private final Path jarPath;

    private final Path outputDir, tempOutputDir;

    public ExtractServerTask() {
        Path cacheDir = getCacheDir();
        Path versionPath = cacheDir.resolve(getMinecraftVersion());

        this.jarPath = versionPath.resolve("server.jar");
        this.outputDir = versionPath.resolve("server");
        this.tempOutputDir = versionPath.resolve("temp-server");
    }

    @TaskAction
    public void extractJar() {
        if (Files.notExists(jarPath))
            throw new IllegalStateException("Server jar does not exist!");

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

    public Path getJarPath() {
        return jarPath;
    }
}
